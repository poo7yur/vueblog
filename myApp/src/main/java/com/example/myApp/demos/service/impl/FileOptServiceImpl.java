package com.example.myApp.demos.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.example.myApp.demos.Constants;
import com.example.myApp.demos.service.FileOptService;
import com.example.myApp.demos.service.ImageService;
import com.example.myApp.demos.util.IpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.regex.Pattern;

@ConditionalOnProperty(
        prefix = "minio",          // 配置前缀
        name = "enabled",          // 配置字段名
        havingValue = "false",     // 匹配的值
        matchIfMissing = true      // 配置缺失时，默认加载这个实现类（兜底）
)
@Service
@Slf4j
public class FileOptServiceImpl implements FileOptService {

    @Resource
    private ImageService imageService;

    @Value("${file.dir}")
    private String fileDir;

    private static final String avatar = "avatar";

    // 注入server.port配置值，若未配置则默认8080（可选默认值）
    @Value("${server.port:8080}")
    private Integer serverPort;

    // 路径遍历攻击校验（禁止../向上跳转，防止写入敏感目录）
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile("\\.\\.");
    // Windows路径分隔符替换（兼容可能传入的Windows路径）
    private static final Pattern WINDOWS_SEPARATOR_PATTERN = Pattern.compile("\\\\");
    /**
     * 上传文件到指定目录
     *
     * @param file 要上传的文件
     * @param dir  目标目录名称
     * @return 上传后的文件共享路径
     * @throws IllegalArgumentException 当文件为空或不存在时抛出
     * @throws Exception                上传过程中可能发生的其他异常
     */
    @Override
    public String uploadFile(MultipartFile file, String dir) throws Exception {
        // 1. 基础校验
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        // 2. 获取原始文件名并清理
        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        if (originalFilename.contains("..")) {
            throw new IllegalArgumentException("文件名包含非法路径");
        }

        String avatarPath = fileDir + "/" + avatar;
        Path uploadDirPath = Paths.get(avatarPath, dir);

        // 3. 如果目录不存在则创建
        if (!Files.exists(uploadDirPath)) {
            Files.createDirectories(uploadDirPath); // createDirectories 支持多级创建
        }

        // 4. 生成唯一文件名 (防止覆盖)
        String fileExtension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            fileExtension = originalFilename.substring(dotIndex);
        }
        String uniqueFileName = RandomUtil.randomNumbers(8) + fileExtension;

        // 5. 构建目标文件路径
        Path targetPath = uploadDirPath.resolve(uniqueFileName);

        try {
            // 6. 执行保存
            file.transferTo(targetPath.toFile());
        } catch (IOException e) {
            throw new Exception("文件保存到磁盘失败", e);
        }

        // 7. 返回访问 URL (注意：本地路径无法直接通过 HTTP 访问，需配合静态资源映射)
        // http://127.0.0.1:8081/static/share/2vwb9473wa_7qsarrc97csko4i32v.png
        String ipPort = "http://" + IpUtil.getLocalIp() + ":" + serverPort;
        return ipPort + "/static/" + avatar + "/" + dir + "/" + uniqueFileName;
    }

    @Override
    public InputStream readFullPathFile(String fullPath) throws Exception {
        InputStream inputStream;
        try {
            inputStream = Files.newInputStream(Paths.get(fullPath), StandardOpenOption.READ);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(Constants.FILE_NOT_FOUND);
        }
        return inputStream;
    }

    /**
     * 写html文本到指定全路径
     *
     * @param fullPath 文件的全路径  /root/essay/20260119/34789.txt
     * @param htmlContent  内容 <h2>content</h2>
     */
    @Override
    public void uploadHtmlFile(String fullPath, String htmlContent) throws Exception {
        // 路径非空校验
        if (fullPath == null || fullPath.trim().isEmpty()) {
            throw new IllegalArgumentException(Constants.PATH_NOT_EMPTY);
        }
        // 内容非空校验（允许空内容，但需提示）
        if (htmlContent == null) {
            htmlContent = "";
        }
        // 路径遍历攻击校验（禁止../，防止写入/root/../etc/passwd等敏感路径）
        String checkPath = WINDOWS_SEPARATOR_PATTERN.matcher(fullPath).replaceAll("/");
        if (PATH_TRAVERSAL_PATTERN.matcher(checkPath).find()) {
            throw new SecurityException(Constants.ILLEGAl_OPT);
        }

        // 替换Windows反斜杠为Linux正斜杠，适配跨系统路径
        String normalizedPath = WINDOWS_SEPARATOR_PATTERN.matcher(fullPath).replaceAll("/");
        // 转为Path对象
        Path targetPath = Paths.get(normalizedPath);
        File targetFile = targetPath.toFile();

        File parentDir = targetFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            boolean mkdirsSuccess = parentDir.mkdirs();
            if (!mkdirsSuccess) {
                throw new IOException("创建父目录失败：" + parentDir.getAbsolutePath());
            }
        }
        try {
            Files.write(
                    targetPath,
                    htmlContent.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
            System.out.println("HTML文件写入成功：" + targetPath.toAbsolutePath());
        } catch (IOException e) {
            throw new IOException("写入文件失败，路径：" + targetPath + "，原因：" + e.getMessage(), e);
        }
    }
}
