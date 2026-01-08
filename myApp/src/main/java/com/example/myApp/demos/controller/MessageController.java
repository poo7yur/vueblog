package com.example.myApp.demos.controller;

import com.example.myApp.demos.aop.AccessLog;
import com.example.myApp.demos.entity.MsgEntity;
import com.example.myApp.demos.entity.R;
import com.example.myApp.demos.service.FileOptService;
import com.example.myApp.demos.service.LogService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
public class MessageController {


    @Resource
    private LogService logService;

    @Resource
    private FileOptService fileOptService;

    @PostMapping("/upload")
    @AccessLog(module = "file manage", description = "upload file")
    public R<String> uploadFile(@RequestPart("file") MultipartFile file) {
        String msg;
        try {
            msg = fileOptService.uploadFile(file);
            return R.ok(msg);
        } catch (Exception e) {
            msg = "上传失败：" + e.getMessage();
            return R.fail(msg);
        }
    }

    @GetMapping("/download")
    @AccessLog(module = "file manage", description = "download file")
    public void downloadFile(@RequestParam("fileId") String fileId, @RequestParam("fileName") String fileName, HttpServletResponse response) {
        try {
            fileOptService.downloadFile(fileId, fileName, response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/getMsg")
    public R<List<MsgEntity>> getMsg(HttpServletRequest request) {
        String userId = request.getHeader("userId");
        try {
            return R.ok(logService.getMsg(userId));
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

}


