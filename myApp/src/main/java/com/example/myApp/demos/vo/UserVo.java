package com.example.myApp.demos.vo;

import lombok.Data;

import java.util.Date;

@Data
public class UserVo {

    private String userId;
    private String name;
    private String email;
    private String phone;
    private Date createTime;
    private String token;
    private String headPicUrl;
}
