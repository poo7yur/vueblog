package com.example.myApp.demos.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Slf4j
public class LocalResourceConfig implements WebMvcConfigurer {

    @Value("${file.dir}")
    private String fileDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String localPath = "file:" + fileDir;
        log.info("mapping localPath: {}", localPath);

        registry
                .addResourceHandler("/static/**")   // URL 访问前缀
                .addResourceLocations(localPath)    // 本地磁盘真实目录
                .setCachePeriod(3600);              // 可选：缓存 1 小时
    }
}
