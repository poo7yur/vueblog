package com.example.myApp.demos.service;

import com.example.myApp.demos.vo.ChatLogVo;
import com.example.myApp.demos.vo.ChatResponse;
import org.springframework.web.multipart.MultipartFile;


public interface ChatService {
    ChatResponse chat(String msg ,String uid);

    ChatLogVo logs(String tmp, String uid);

    String draw(String msg, MultipartFile[] files, String uid) throws Exception;

    String query(String taskId);

}
