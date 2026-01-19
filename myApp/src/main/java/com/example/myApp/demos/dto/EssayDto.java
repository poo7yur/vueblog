package com.example.myApp.demos.dto;

import lombok.Data;

@Data
public class EssayDto {

    private String storagePath;
    private String id;
    private int status;
    private String content;
}
