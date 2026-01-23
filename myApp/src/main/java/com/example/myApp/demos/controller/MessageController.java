package com.example.myApp.demos.controller;

import cn.hutool.core.util.RandomUtil;
import com.example.myApp.demos.dto.PageDto;
import com.example.myApp.demos.dto.PhoneDto;
import com.example.myApp.demos.entity.MsgEntity;
import com.example.myApp.demos.entity.R;
import com.example.myApp.demos.service.LogService;
import com.github.pagehelper.PageInfo;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;


@RestController
public class MessageController {

    @Resource
    private LogService logService;

    @PostMapping("/sendNotice")
    public void sendNotice(@RequestBody PhoneDto phoneDto) {
         try {
             //todo 调运营商接口发手机验证码
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


