package com.example.myApp.demos.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
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
    private String headPicUrl;

    public User(String userId ,String name){
        this.userId = userId;
        this.name = name;
    }

}
