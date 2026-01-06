package com.example.myApp.demos.entity;

import lombok.Data;

import java.util.Date;

@Data
public class User {

    private String userId;
    private String name;
    private String email;
    private String password;
    private String salt;
    private String phone;
    private Date createTime;
    private String role;
    private int status;
    private double defaultMb;
}
