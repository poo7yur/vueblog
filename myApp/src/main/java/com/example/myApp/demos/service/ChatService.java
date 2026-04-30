package com.example.myApp.demos.service;

import com.example.myApp.demos.vo.ChatLogVo;
import com.example.myApp.demos.vo.ChatResponse;


public interface ChatService {
    ChatResponse chat(String msg ,String uid);

    ChatLogVo logs(String tmp, String uid);

}
