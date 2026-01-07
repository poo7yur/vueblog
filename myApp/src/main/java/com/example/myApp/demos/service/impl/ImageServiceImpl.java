package com.example.myApp.demos.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.example.myApp.demos.dto.DirDto;
import com.example.myApp.demos.dto.ImageDto;
import com.example.myApp.demos.entity.User;
import com.example.myApp.demos.mapper.UserMapper;
import com.example.myApp.demos.service.ImageService;
import com.example.myApp.demos.util.DirScannerUtil;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class ImageServiceImpl implements ImageService {

    @Value("${file.dir}")
    private String fileDir;

    @Resource
    private UserMapper userMapper;

    /**
     * 扫描指定路径下的目录结构并构建树形节点
     *
     * @param path    要扫描的目录路径，如果为空则使用默认文件目录 如果有值则是当前用户路径下
     * @param request HTTP请求对象，用于解析用户登录状态
     * @return 包含目录结构的树形节点对象
     */
    @Override
    public DirScannerUtil.Node scanner(String path, HttpServletRequest request) throws IOException {
        boolean isPath = StringUtils.isEmpty(path);
        String fullPath = isPath ? fileDir : fileDir + path;
        //如果是当前用户直接进他的空间不过滤
        if (!isPath) return DirScannerUtil.scannerTree(fullPath);
        //用户名集合
        Set<String> userNames = userMapper.queryNames();
        //解析登录状态
        String loginUser = parseUserFromToken(request);
        //递归目录
        return DirScannerUtil.buildTree(Paths.get(fullPath), loginUser, userNames);
    }

    @Override
    public PageImageVo listImages(ImageDto dto) {
        List<String> imgUrls = new ArrayList<>();
        int total = DirScannerUtil.listImages(dto, imgUrls);
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
        String currentPath = dto.getCurrentPath();
        String folderName = dto.getFolderName();
        Path newPath = Paths.get(currentPath, folderName);
        Files.createDirectories(newPath);
    }

    @Override
    public void deleteDir(DirDto dto) throws IOException {
        Path target = Paths.get(dto.getCurrentPath());
        String user = dto.getCurrentUser();
        String userSpace = fileDir + user;
        if (!userSpace.startsWith(fileDir) && dto.getCurrentPath().length() > userSpace.length())
            throw new RuntimeException("非法操作");
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

    private String parseUserFromToken(HttpServletRequest request) {
        String token = request.getHeader("token");
        if (StringUtils.isBlank(token)) {
            return null;
        }
        try {
            Claims claims = JwtUtil.parseToken(token);
            return claims.getSubject();
        } catch (Exception e) {
            return null;
        }
    }
}
