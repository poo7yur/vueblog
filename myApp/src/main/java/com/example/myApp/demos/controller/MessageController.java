package com.example.myApp.demos.controller;

import com.example.myApp.demos.aop.AccessLog;
import com.example.myApp.demos.entity.OrderDto;
import com.example.myApp.demos.mq.MyLogger;
import com.example.myApp.demos.service.FileOptService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

@RestController
public class MessageController {

    @Resource
    private MyLogger myLogger;

    @Resource
    private FileOptService fileOptService;

    @GetMapping("/do")
    public String doSomething() {
        myLogger.log("INFO", "querySomething");
        return "ok";
    }

    @PostMapping("/order")
    @AccessLog(module = "order manage", description = "create order")
    public String order(@RequestBody OrderDto order) {
        return "create order done";
    }

    @PostMapping("/upload")
    @AccessLog(module = "file manage", description = "upload file")
    public String uploadFile(@RequestPart("file") MultipartFile file) {
        try {
            return fileOptService.uploadFile(file);
        } catch (Exception e) {
            return "上传失败：" + e.getMessage();
        }
    }

    @GetMapping("/download")
    @AccessLog(module = "file manage", description = "download file")
    public String downloadFile(@RequestParam("fileId") String fileId, @RequestParam("fileName") String fileName, HttpServletResponse response) {
        try {
            fileOptService.downloadFile(fileId, fileName, response);
            return "下载成功";
        } catch (Exception e) {
            return "下载失败: " + e.getMessage();
        }
    }

}


