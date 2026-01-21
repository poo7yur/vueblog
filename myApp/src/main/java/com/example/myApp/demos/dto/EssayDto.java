package com.example.myApp.demos.dto;

import lombok.Data;

@Data
public class EssayDto {

    private String id;
    private String storagePath;
    private int status;
    private String content;
    private String title;
}
