package com.example.myApp.demos.entity;

import lombok.Data;

@Data
public class MsgEntity {

    private String msgId;
    private String msgContent;
    private String updateTime;
    private int state;//0未读 1已读 2失效
    private String createBy;//消息创建人
    private String groupId;//分组
    private String userId;//消息接收人
    private String msgType;//0 点赞 1评论
}
