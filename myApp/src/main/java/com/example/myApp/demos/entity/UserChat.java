package com.example.myApp.demos.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserChat {
    private String chatId;
    private String userId;
    private String msg;
    private String answer;
}
