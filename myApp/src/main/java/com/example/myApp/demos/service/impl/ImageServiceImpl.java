package com.example.myApp.demos.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.example.myApp.demos.Constants;
import com.example.myApp.demos.dto.DirDto;
import com.example.myApp.demos.dto.ImageDto;
import com.example.myApp.demos.dto.OptDto;
import com.example.myApp.demos.entity.ShareImage;
import com.example.myApp.demos.entity.User;
import com.example.myApp.demos.mapper.ImageMapper;
import com.example.myApp.demos.mapper.UserMapper;
import com.example.myApp.demos.service.ImageService;
import com.example.myApp.demos.util.DirUtil;
import com.example.myApp.demos.util.JwtUtil;
import com.example.myApp.demos.vo.PageImageVo;
import io.jsonwebtoken.Claims;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

@Service
public class ImageServiceImpl implements ImageService {

    @Value("${file.dir}")
    private String fileDir;

    @Resource
    private UserMapper userMapper;

    @Resource
    private ImageMapper imageMapper;

    /**
     * 扫描指定路径下的目录结构并构建树形节点
     *
     * @param path    要扫描的目录路径，如果为空则使用默认文件目录 如果有值则是当前用户路径下
     * @param request HTTP请求对象，用于解析用户登录状态
     * @return 包含目录结构的树形节点对象
     */
    @Override
    public DirUtil.Node scanner(String path, HttpServletRequest request) throws IOException {
        boolean isPath = StringUtils.isEmpty(path);
        String fullPath = isPath ? fileDir : fileDir + path;
        //如果是当前用户直接进他的空间不过滤
        if (!isPath) return DirUtil.scannerTree(fullPath);
        //用户名集合
        Set<String> userNames = userMapper.queryNames();
        //解析登录状态
        String loginUser = parseUserFromToken(request);
        //递归目录
        return DirUtil.buildTree(Paths.get(fullPath), loginUser, userNames);
    }

    @Override
    public PageImageVo listImages(ImageDto dto) {
        List<String> imgUrls = new ArrayList<>();
        int total = DirUtil.listImages(dto, imgUrls);
        PageImageVo pageImageVo = new PageImageVo();
        BeanUtils.copyProperties(dto, pageImageVo);
        pageImageVo.setTotal(total);
        pageImageVo.setImages(imgUrls);
        return pageImageVo;
    }

    /**
     * 上传图片到服务器指定路径下
     * destPath= '/adm/02/021/'
     */
    @Override
    public void uploadImage(MultipartFile file, String destPath) throws IOException {
        String userName = destPath.split("/")[1];
        if (StringUtils.isEmpty(userName)) throw new IllegalArgumentException("文件格式非法");
        Path userSpace = Paths.get(fileDir, userName);
        //判断目标路径下是否有子目录 有的话不让放图片
        if (checkDirHasChildDir(destPath)) throw new RuntimeException("目标目录下不允许放图片");
        long div = 1024 * 1024;
        /*计算已用空间 & 校验额度（user表里配了额度，单位 MB） */
        long usedMb = Files.exists(userSpace)
                ? Files.walk(userSpace)
                .filter(Files::isRegularFile)
                .mapToLong(p -> p.toFile().length())
                .sum() / div
                : 0;
        User user = userMapper.findUser(userName);
        double defaultMb = user.getDefaultMb();
        long fileSizeMb = file.getSize() / div;
        double availableMb = defaultMb - usedMb;
        String tip = "还剩余：" + availableMb + "mb";
        if (usedMb + fileSizeMb > defaultMb) throw new RuntimeException("您的空间不足" + tip);
        /*使用相对路径 目标目录不存在则一次性建好（支持多级） */
        String relative = destPath.replaceFirst("^/", "");
        if (relative.startsWith(userName)) {
            relative = relative.substring(userName.length()).replaceFirst("^/", "");
        }
        Path targetDir = userSpace.resolve(relative);
        Files.createDirectories(targetDir);
        String origName = file.getOriginalFilename();
        String ext = "";
        if (origName != null && origName.lastIndexOf('.') != -1) {
            ext = origName.substring(origName.lastIndexOf('.'));
        }
        String newName = RandomUtil.randomString(18) + ext;
        //真正写盘
        Path targetFile = targetDir.resolve(newName);
        file.transferTo(targetFile.toFile());
    }

    @Override
    public void createDir(DirDto dto) throws IOException {
        String currentPathStr = dto.getCurrentPath();
        String folderName = dto.getFolderName();

        Path currentDir = Paths.get(currentPathStr);
        Path newPath = currentDir.resolve(folderName); // 更安全的路径拼接

        // 检查当前目录是否存在且是目录
        if (!Files.exists(currentDir)) {
            throw new RuntimeException("当前路径不存在: " + currentPathStr);
        }
        if (!Files.isDirectory(currentDir)) {
            throw new RuntimeException("当前路径不是目录: " + currentPathStr);
        }

        // 检查当前目录下是否有文件（不包括子目录）
        try (Stream<Path> stream = Files.list(currentDir)) {
            // 只判断普通文件
            boolean hasFiles = stream.anyMatch(Files::isRegularFile);

            if (hasFiles) {
                throw new IllegalArgumentException("当前目录包含文件，不允许创建新子文件夹");
            }
        }
        // 检查要创建的目录是否已存在
        if (Files.exists(newPath)) throw new RuntimeException("目录已存在: " + newPath);
        // 创建新目录
        Files.createDirectories(newPath);
    }

    @Override
    public void deleteDir(DirDto dto) throws IOException {
        Path target = Paths.get(dto.getCurrentPath());
        String user = dto.getCurrentUser();
        String userSpace = fileDir + user;
        if (!userSpace.startsWith(fileDir) && dto.getCurrentPath().length() > userSpace.length())
            throw new RuntimeException(Constants.ILLEGAl_OPT);
        if (!Files.exists(target) || !Files.isDirectory(target))
            throw new FileNotFoundException("目录不存在：" + target);
        Files.walk(target)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException e) {
                        throw new RuntimeException("删除失败：" + p, e);
                    }
                });
    }

    @Override
    public String removeImage(OptDto dto, HttpServletRequest request) throws IOException {
        // 1. 拿到真实文件路径
        Path targetPath = parseRealPath(dto.getPath());
        // 2. 安全校验：必须位于当前登录用户的空间内
        String username = parseUserFromToken(request);
        Path userSpace = Paths.get(fileDir, username).toAbsolutePath();
        if (!targetPath.toAbsolutePath().startsWith(userSpace)) throw new RuntimeException(Constants.ILLEGAl_OPT);
        // 3. 删除
        Files.deleteIfExists(targetPath);
        return "删除成功";
    }

    @Override
    public String shareImage(OptDto dto, HttpServletRequest request) throws IOException {
        // 从token里校验当前用户是否有权限
        String username = parseUserFromToken(request);
        if (StringUtils.isEmpty(username)) throw new RuntimeException(Constants.TOKEN_EXPIRE);
        Path fullPath = parseRealPath(dto.getPath());
        Path userSpace = Paths.get(fileDir, username).toAbsolutePath();
        if (!Files.exists(fullPath)) throw new RuntimeException(Constants.FILE_NOT_FOUND);
        if (!fullPath.toAbsolutePath().startsWith(userSpace)) throw new RuntimeException(Constants.ILLEGAl_OPT);

        // 写share_image表记录共享操作
        String uid = parseUidFromToken(request);
        ShareImage shareImage = new ShareImage();
        String rid = RandomUtil.randomString(10);
        shareImage.setId(rid);
        shareImage.setOwnerId(uid);
        shareImage.setFlg(0);
        shareImage.setUpdateTime(new Date());
        shareImage.setSourcePath(fullPath.toString());
        shareImage.setRemark(dto.getRemark());

        // 把图片复制一份放到share目录下
        Path shareDir = Paths.get(fileDir, "share");
        if (!Files.exists(shareDir)) Files.createDirectories(shareDir);
        String fileName = fullPath.getFileName().toString();
        String copyFileName = rid + "_" + fileName;
        Path targetPath = shareDir.resolve(copyFileName);
        Files.copy(fullPath, targetPath);
        String sharePath = targetPath.toAbsolutePath().toString();
        shareImage.setSharePath(sharePath);
        imageMapper.shareImg(shareImage);
        return sharePath;
    }

    private boolean checkDirHasChildDir(String dirPath) {
        Path dir = Paths.get(fileDir + dirPath);
        // 检查路径是否存在且是目录
        if (!Files.exists(dir) || !Files.isDirectory(dir)) return false;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path path : stream) {
                if (Files.isDirectory(path)) {
                    return true; // 找到第一个子目录就返回 true
                }
            }
        } catch (IOException e) {
            return false;
        }
        return false; // 没有子目录
    }

    private Path parseRealPath(String staticPath) {
        if (!staticPath.startsWith("/static/")) throw new RuntimeException(Constants.ILLEGAl_OPT);
        // 去掉 /static 前缀得到用户路径
        String relative = staticPath.substring("/static".length());
        return Paths.get(fileDir, relative).normalize(); //返回文件全路径
    }

    private String parseUserFromToken(HttpServletRequest request) {
        String token = request.getHeader("token");
        if (StringUtils.isBlank(token)) {
            return null;
        }
        try {
            Claims claims = JwtUtil.parseToken(token);
            return claims.getSubject();//返回用户名
        } catch (Exception e) {
            return null;
        }
    }

    private String parseUidFromToken(HttpServletRequest request) {
        String token = request.getHeader("token");
        if (StringUtils.isBlank(token)) {
            return null;
        }
        try {
            Claims claims = JwtUtil.parseToken(token);
            return claims.get("userId").toString(); //返回用户id
        } catch (Exception e) {
            return null;
        }
    }
}
