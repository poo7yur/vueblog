package com.example.myApp.demos.controller;

import com.example.myApp.demos.entity.R;
import com.example.myApp.demos.service.ChatService;
import com.example.myApp.demos.util.JwtUtil;
import com.example.myApp.demos.vo.ChatLogVo;
import com.example.myApp.demos.vo.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

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
}
