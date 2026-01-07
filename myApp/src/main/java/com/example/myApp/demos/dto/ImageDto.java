package com.example.myApp.demos.dto;

import lombok.Data;

@Data
public class ImageDto {

    private String path;

    private String userId;

    private int pageSize=10;

    private int pageNo=1;

}
