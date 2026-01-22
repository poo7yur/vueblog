package com.example.myApp.demos.controller;

import cn.hutool.core.util.RandomUtil;
import com.example.myApp.demos.aop.AccessLog;
import com.example.myApp.demos.dto.PageDto;
import com.example.myApp.demos.dto.PhoneDto;
import com.example.myApp.demos.entity.MsgEntity;
import com.example.myApp.demos.entity.R;
import com.example.myApp.demos.service.FileOptService;
import com.example.myApp.demos.service.LogService;
import com.github.pagehelper.PageInfo;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
public class MessageController {


    @Resource
    private LogService logService;

    @Resource
    private FileOptService fileOptService;

    @GetMapping("/download")
    @AccessLog(module = "file manage", description = "download file")
    public void downloadFile(@RequestParam("fileId") String fileId, @RequestParam("fileName") String fileName, HttpServletResponse response) {
        try {
            fileOptService.downloadFile(fileId, fileName, response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/sendNotice")
    public void sendNotice(@RequestBody PhoneDto phoneDto) {
         try {
             System.out.println(phoneDto.getPhone() +"验证码是：" + RandomUtil.randomNumbers(6));
         } catch (Exception e) {
             throw new RuntimeException(e);
         }
    }

    @PostMapping("/getMsg")
    public R<PageInfo<MsgEntity>> getMsg(HttpServletRequest request, @RequestBody PageDto dto) {
        try {
            return R.ok(logService.getMsg(dto ,request));
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

}


