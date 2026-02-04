package com.example.myApp.demos.service.impl;

import com.example.myApp.demos.service.FileOptService;
import io.minio.*;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

@Service
@Data
@ConditionalOnProperty(
        prefix = "minio",
        name = "enabled",
        havingValue = "true"
)
public class MinioFileOptServiceImpl implements FileOptService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    public MinioFileOptServiceImpl(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @Override
    public String uploadAvatarFile(MultipartFile file, String objName) throws Exception {
        //检查桶是否存在 不在新建桶
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }

        String originalFilename = file.getOriginalFilename();
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
        }
        //返回共享url
        return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .bucket(bucket)
                .method(Method.GET)
                .expiry(7 * 24 * 3600)
                .object(objName).extraQueryParams(getExtraPrams(originalFilename))
                .build());
    }


    @Override
    public InputStream readFullPathFile(String fullPath) throws Exception {

        String cleanPath = fullPath.startsWith("/") ? fullPath.substring(1) : fullPath;

        // 解析桶（bucket）和对象名称（objectName）
        String[] pathSegments = cleanPath.split("/");
        if (pathSegments.length < 2) {
            throw new IllegalArgumentException("文件路径格式非法");
        }
        String bucketName = pathSegments[0]; // 提取桶名：root
        // 拼接对象名称（bucket之后的所有路径，即 essay/20260119/08487.txt）
        StringBuilder objectName = new StringBuilder();
        for (int i = 1; i < pathSegments.length; i++) {
            objectName.append(pathSegments[i]);
            if (i != pathSegments.length - 1) {
                objectName.append("/");
            }
        }

        // 校验桶是否存在（可选，增强容错性）
        if (!minioClient.bucketExists(io.minio.BucketExistsArgs.builder().bucket(bucketName).build())) {
            throw new RuntimeException("MinIO桶不存在：" + bucketName);
        }

        // 从MinIO获取文件输入流
        GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                .bucket(bucketName) // 桶名
                .object(objectName.toString()) // 存储对象名称（文件在桶内的路径）
                .build();

        // 6. 返回文件输入流（注意：流的关闭由调用方负责，避免资源泄漏）
        return minioClient.getObject(getObjectArgs);
    }

    @Override
    public void uploadHtmlFile(String fullPath, String htmlContent) throws Exception {
        String cleanPath = fullPath.startsWith("/") ? fullPath.substring(1) : fullPath;
        // 解析桶（bucket）和对象名称（objectName）
        String[] pathSegments = cleanPath.split("/");
        if (pathSegments.length < 2) {
            throw new IllegalArgumentException("文件路径格式非法");
        }
        String bucketName = pathSegments[0]; // 提取桶名：root
        // 拼接对象名称（bucket之后的所有路径，即 essay/20260119/08487.txt）
        StringBuilder objectName = new StringBuilder();
        for (int i = 1; i < pathSegments.length; i++) {
            objectName.append(pathSegments[i]);
            if (i != pathSegments.length - 1) {
                objectName.append("/");
            }
        }

        if (!minioClient.bucketExists(io.minio.BucketExistsArgs.builder().bucket(bucketName).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());  // 创建桶
        }

        // 将字符串转换为输入流
        byte[] contentBytes = htmlContent.getBytes(StandardCharsets.UTF_8);
        try (InputStream inputStream = new ByteArrayInputStream(contentBytes)) {

            // 执行上传操作
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName.toString())
                            .stream(inputStream, contentBytes.length, -1) // -1 表示不限制流的大小（由MinIO自动处理）
                            .contentType("text/html; charset=utf-8") // 设置内容类型为HTML
                            .build()
            );

            System.out.println("文件上传成功: " + bucketName + "/" + objectName);
        } catch (MinioException | IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    private Map<String, String> getExtraPrams(String fileName) {
        Map<String, String> extraParams = new HashMap<>();
        if (".jpg".endsWith(fileName.toLowerCase()) || fileName.toLowerCase().endsWith(".jpeg")) {
            extraParams.put("response-content-type", "image/jpeg");
        } else if (fileName.toLowerCase().endsWith(".png")) {
            extraParams.put("response-content-type", "image/png");
        } else if (fileName.toLowerCase().endsWith(".txt")) {
            extraParams.put("response-content-type", "text/plain; charset=UTF-8");
        }
        return extraParams;
    }

    private void writeByStream(String fileName, InputStream inputStream, HttpServletResponse response) throws IOException {
        // 解决中文文件名乱码问题
        String encodedFileName = URLEncoder.encode(fileName, String.valueOf(StandardCharsets.UTF_8));
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);

        //将文件流写回前端
        byte[] buffer = new byte[1024];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            response.getOutputStream().write(buffer, 0, len);
        }
        response.flushBuffer();
        inputStream.close();
    }
}
