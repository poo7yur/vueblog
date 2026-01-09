package com.example.myApp.demos.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import com.example.myApp.demos.Constants;
import com.example.myApp.demos.dto.DirDto;
import com.example.myApp.demos.dto.ImageDto;
import com.example.myApp.demos.dto.LikeNotice;
import com.example.myApp.demos.dto.OptDto;
import com.example.myApp.demos.entity.ShareImage;
import com.example.myApp.demos.entity.User;
import com.example.myApp.demos.entity.UserImgRel;
import com.example.myApp.demos.mapper.ImageMapper;
import com.example.myApp.demos.mapper.UserMapper;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import com.example.myApp.demos.service.ImageService;
import com.example.myApp.demos.util.DirUtil;
import com.example.myApp.demos.util.JwtUtil;
import com.example.myApp.demos.vo.PageImageVo;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Stream;

@Service
@Slf4j
public class ImageServiceImpl implements ImageService {

    @Value("${file.dir}")
    private String fileDir;

    @Resource
    private UserMapper userMapper;

    @Resource
    private ImageMapper imageMapper;

    @Resource
    private DefaultMQProducer rocketMQProducer;

    private static final SimpleDateFormat sdf = new  SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

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
    public void uploadImage(List<MultipartFile> files, String destPath) throws IOException {
        // 1. 前置校验：文件不能为空
        if (files == null || files.isEmpty()) throw new IllegalArgumentException("待上传文件不能为空");
        // 2. 解析用户名并校验
        String[] pathSegments = destPath.split("/");
        if (pathSegments.length < 2 || StringUtils.isEmpty(pathSegments[1])) {
            throw new IllegalArgumentException(Constants.ILLEGAl_OPT);
        }
        String userName = pathSegments[1];

        // 3. 校验目标目录：不允许有子目录
        if (checkDirHasChildDir(destPath)) throw new RuntimeException(Constants.NOT_ALLOWED);

        // 4. 空间校验相关常量
        long div = 1024 * 1024; // 1MB = 1024*1024 字节
        Path userSpace = Paths.get(fileDir, userName);

        // 4.1 计算用户已使用空间（MB）
        long usedMb = Files.exists(userSpace)
                ? Files.walk(userSpace)
                .filter(Files::isRegularFile)
                .mapToLong(p -> p.toFile().length())
                .sum() / div
                : 0;

        // 4.2 获取用户空间额度
        User user = userMapper.findUser(userName);
        if (user == null) throw new RuntimeException(Constants.USER_NOT_FIND);
        double defaultMb = user.getDefaultMb();
        double availableMb = defaultMb - usedMb;
        String tip = "还剩余：" + availableMb + "MB";

        // 4.3 计算所有待上传文件的总大小（MB）
        long totalFileSizeMb = 0;
        for (MultipartFile file : files) {
            // 过滤空文件
            if (file.isEmpty()) {
                continue;
            }
            totalFileSizeMb += file.getSize() / div;
            // 额外校验：单个文件大小（可选，根据业务需求添加）
            if (file.getSize() / div > 10) throw new RuntimeException("单个文件大小不能超过10MB");
        }

        // 4.4 校验总空间是否足够
        if (usedMb + totalFileSizeMb > defaultMb)
            throw new RuntimeException("您的空间不足，" + tip + "，本次上传需要" + totalFileSizeMb + "MB");

        // 5. 构建目标目录路径
        String relative = destPath.replaceFirst("^/", "");
        if (relative.startsWith(userName)) {
            relative = relative.substring(userName.length()).replaceFirst("^/", "");
        }
        Path targetDir = userSpace.resolve(relative);
        // 确保目标目录存在（支持多级目录）
        Files.createDirectories(targetDir);

        for (MultipartFile file : files) {
            // 跳过空文件
            if (file.isEmpty()) {
                continue;
            }

            try {
                // 6.1 处理文件名：生成随机名 + 保留原后缀
                String origName = file.getOriginalFilename();
                String ext = "";
                if (origName != null && origName.lastIndexOf('.') != -1) {
                    ext = origName.substring(origName.lastIndexOf('.'));
                }
                String newName = RandomUtil.randomString(18) + ext;

                // 6.2 写盘：保存文件到目标目录
                Path targetFile = targetDir.resolve(newName);
                file.transferTo(targetFile.toFile());

            } catch (Exception e) {
                // 异常处理策略：中断全部上传（原子性）
                throw new RuntimeException("文件上传失败：" + file.getOriginalFilename() + "，原因：" + e.getMessage(), e);
            }
        }
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
                throw new IllegalArgumentException(Constants.NOT_ALLOWED);
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

    @Override
    public String likeImage(OptDto dto, HttpServletRequest request) throws IOException {
        // 1. 基础参数校验
        if (dto == null) {
            throw new IllegalArgumentException(Constants.PARM_NOT_NULL);
        }
        String path = dto.getPath();
        if (StringUtils.isEmpty(path)) {
            throw new IllegalArgumentException(Constants.PATH_NOT_EMPTY);
        }

        // 2. 从token解析用户ID并校验
        String userId = parseUidFromToken(request);
        if (StringUtils.isEmpty(userId)) {
            throw new RuntimeException(Constants.TOKEN_EXPIRE);
        }

        // 3. 解析图片ID（增加格式校验）
        String[] str = path.split("_");
        if (str.length < 2) {
            log.error("点赞图片失败：path格式错误，path={}", path);
            throw new IllegalArgumentException("图片路径格式错误，正确格式应为：图片ID_图片名称");
        }
        String imgID = str[0];
        if (StringUtils.isEmpty(imgID)) {
            log.error("点赞图片失败：解析出的图片ID为空，path={}", path);
            throw new IllegalArgumentException("图片ID不能为空");
        }

        // 4. 防重复点赞校验
        boolean isLiked = imageMapper.checkUserImgRelExists(imgID, userId) >= 1;
        if (isLiked) {
            return "已点赞图片无需重复操作";
        }

        // 5. 记录用户-图片点赞关系
        try {
            UserImgRel userImgRel = new UserImgRel(imgID, userId);
            imageMapper.recordUserImgRel(userImgRel);
            log.info("用户{}成功点赞图片{}", userId, imgID);
        } catch (Exception e) {
            throw new RuntimeException("点赞失败：记录点赞关系异常", e);
        }

        // 6. 查询图片拥有者ID
        String ownerId = imageMapper.queryOwnerId(imgID);
        // 7. 发送RocketMQ点赞通知
        String time = sdf.format(new Date());
        sendLikeNoticeMessage(new LikeNotice(imgID, str[1], userId, ownerId, time));
        return "点赞成功";
    }

    private void sendLikeNoticeMessage(LikeNotice likeNotice) {
        try {
            // 构建消息内容（JSON格式更易解析，建议使用FastJSON/Jackson序列化）
            String noticeContent = JSON.toJSONString(likeNotice);

            // 创建RocketMQ消息
            Message message = new Message(
                    Constants.IMAGE_LIKE_NOTICE_TOPIC, // 主题
                    "IMAGE_LIKE_TAG",  // 标签（便于消息过滤）
                    noticeContent.getBytes(StandardCharsets.UTF_8)
            );

            // 发送消息
            SendResult sendResult = rocketMQProducer.send(message);
        } catch (Exception e) {
            // 消息发送失败不影响核心业务（点赞已成功），仅记录日志，可根据业务需求选择是否重试
            throw new RuntimeException("消息发送失败", e); // 如需强一致性可抛出异常
        }
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
