package com.example.myApp.demos.vo;

import lombok.Data;

import java.util.List;

@Data
public class PageImageVo {

    private List<String> urls;
    private int total;
    private int pageNo;
    private int pageSize;
}
