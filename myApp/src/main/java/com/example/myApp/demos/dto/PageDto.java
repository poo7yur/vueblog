package com.example.myApp.demos.dto;

import lombok.Data;

@Data
public class PageDto {

    private String keyword;
    private Integer pageNum;
    private Integer pageSize;
    private String userId;
    private int type=0;
}
