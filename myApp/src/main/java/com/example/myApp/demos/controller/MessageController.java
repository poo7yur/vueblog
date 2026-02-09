package com.example.myApp.demos.controller;

import cn.hutool.core.util.RandomUtil;
import com.example.myApp.demos.dto.PageDto;
import com.example.myApp.demos.dto.PhoneDto;
import com.example.myApp.demos.entity.MsgEntity;
import com.example.myApp.demos.entity.R;
import com.example.myApp.demos.service.LogService;
import com.example.myApp.demos.util.JwtUtil;
import com.example.myApp.demos.vo.SongVo;
import com.github.pagehelper.PageInfo;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;


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
    @GetMapping("/listSongs")
    public R<List<SongVo>> listSongs(HttpServletRequest request){
        try {
            return R.ok(logService.listSongs(JwtUtil.parseUid(request)));
        } catch (Exception e){
            return R.fail(e.getMessage());
        }
    }

    @GetMapping("/getSong")
    public void getSong(HttpServletResponse response ,@RequestParam("path") String path){
        try {
            // 将输入流数据写入响应输出流
            try (FileInputStream ins = new FileInputStream(path); OutputStream os = response.getOutputStream()) {
                response.setContentType("application/octet-stream"); // 通用二进制流类型

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = ins.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
            } catch (IOException e) {
                throw new RuntimeException("文件传输失败: " + e.getMessage());
            }
            // 确保输入流关闭
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}


