package com.example.myApp.demos.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentEntity {

    private String id;
    private String content;
    private String commentBy;
    private String commentTime;
    private int flg;
    private String imgId;
}
