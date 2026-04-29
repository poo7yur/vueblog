package com.example.myApp.demos.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatResponse {
    private String chatId;
    private String thinkContent;
    private String summaryContent;
    private String answerStatus;
}
