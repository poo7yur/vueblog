package com.example.myApp.demos.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserFollow {

    private long id;
    private String followId;//关注者ID
    private String followingId;//被关注者ID
    private int status;//1关注中 0已取消 -1拉黑
    private Date createTime;
    private Date updateTime;
}
