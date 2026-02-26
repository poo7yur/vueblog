package com.example.myApp.demos.entity;

import lombok.Data;

import java.util.Date;

@Data
public class Tag {

    private String tagId;
    private String tagName;
    private String description;
    private int status;
    private int isPublic;
    private Date updateTime;
    private String updateBy;
    private String color;
    private String pid;
}
