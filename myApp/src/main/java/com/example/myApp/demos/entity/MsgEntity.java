package com.example.myApp.demos.entity;

import lombok.Data;

import java.util.Date;

@Data
public class MsgEntity {

    private String msgId;
    private String msgContent;
    private Date updateTime;
    private int state;
    private String createBy;
    private String groupId;
    private int msgType;
}
