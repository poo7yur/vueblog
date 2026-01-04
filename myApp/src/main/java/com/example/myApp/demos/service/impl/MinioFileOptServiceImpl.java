package com.example.myApp.demos.service.impl;

import com.example.myApp.demos.service.FileOptService;
import io.minio.*;
import io.minio.http.Method;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
@Data
public class MinioFileOptServiceImpl implements FileOptService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    public MinioFileOptServiceImpl(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @Override
    public String uploadFile(MultipartFile file) throws Exception {
        //检查桶是否存在 不在新建桶
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
        long dir = System.currentTimeMillis();
        String originalFilename = file.getOriginalFilename();
        String fileName = dir + "/" + originalFilename;
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(fileName)
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
                .object(fileName).extraQueryParams(getExtraPrams(originalFilename))
                .build());
    }

    @Override
    public void downloadFile(String fileId, String fileName, HttpServletResponse response) throws Exception {
        String objName = fileId + "/" + fileName;
        InputStream inputStream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucket)
                .object(objName)
                .build());
        writeByStream(fileName, inputStream, response);
    }

    private Map<String ,String> getExtraPrams(String fileName){
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
