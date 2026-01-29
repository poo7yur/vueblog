package com.example.myApp.demos.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserFollow {

    private long id;
    private String followId;
    private String followingId;
    private int status;
    private Date createTime;
    private Date updateTime;
}
