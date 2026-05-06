package com.example.myApp.demos.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserChat {
    private String chatId;
    private String userId;
    private String msg;
    private String answer;
    private String taskId;
    private String type;

    public UserChat(String userId, String chatId, String msg, String answer, String type) {
        this.userId = userId;
        this.chatId = chatId;
        this.answer = answer;
        this.msg = msg;
        this.type = type;
    }
}
