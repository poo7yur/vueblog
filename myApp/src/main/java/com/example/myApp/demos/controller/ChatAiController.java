package com.example.myApp.demos.controller;

import com.example.myApp.demos.entity.R;
import com.example.myApp.demos.service.ChatService;
import com.example.myApp.demos.vo.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/ai")
@RestController
public class ChatAiController {

    @Autowired
    private ChatService chatService;

    @GetMapping("/chat")
    public R<ChatResponse> chat(@RequestParam String msg) {
        return R.ok(chatService.chat(msg));
    }
}
