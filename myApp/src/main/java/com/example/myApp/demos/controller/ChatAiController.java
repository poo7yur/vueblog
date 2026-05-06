package com.example.myApp.demos.controller;

import com.example.myApp.demos.entity.R;
import com.example.myApp.demos.service.ChatService;
import com.example.myApp.demos.util.JwtUtil;
import com.example.myApp.demos.vo.ChatLogVo;
import com.example.myApp.demos.vo.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RequestMapping("/ai")
@RestController
public class ChatAiController {

    @Autowired
    private ChatService chatService;

    @GetMapping("/chat")
    public R<ChatResponse> chat(@RequestParam String msg , HttpServletRequest request) {
        try {
            String uid = JwtUtil.parseUid(request);
            return R.ok(chatService.chat(msg, uid));
        }  catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @GetMapping("/logs")
    public R<ChatLogVo> logs(@RequestParam(required = false) String timestp, HttpServletRequest request) {
        try {
            String uid = JwtUtil.parseUid(request);
            return R.ok(chatService.logs(timestp, uid));
        } catch (Exception e){
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/draw")
    public R<String> draw(@RequestParam String msg , MultipartFile[] files ,HttpServletRequest request) {
        try {
            String uid = JwtUtil.parseUid(request);
            return R.ok(chatService.draw(msg ,files ,uid));
        } catch (Exception e){
            return R.fail(e.getMessage());
        }
    }

    @GetMapping("/query")
    public R<String> query(@RequestParam String taskId){
        try {
            return R.ok(chatService.query(taskId));
        } catch (Exception e){
            return R.fail(e.getMessage());
        }
    }

    @GetMapping("/download")
    public void download(@RequestParam String taskId , HttpServletResponse response){

    }
}
