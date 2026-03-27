package com.example.myApp.demos.entity;

import lombok.Data;

import java.util.Date;

@Data
public class UserRssRel {
    private long id;
    private String userId;
    private String rssUrl;
    private int enable;
    private String corn;
    private String remark;
    private Date createTime;
    private Date updateTime;
}
