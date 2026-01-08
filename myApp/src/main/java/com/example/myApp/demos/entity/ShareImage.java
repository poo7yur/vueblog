package com.example.myApp.demos.entity;

import lombok.Data;

import java.util.Date;

@Data
public class ShareImage {

    private String id;
    private String sourcePath;
    private int flg;
    private String ownerId;
    private Date updateTime;
    private String remark;
    private String sharePath;
}
