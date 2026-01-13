package com.example.myApp.demos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentNotice implements Serializable {

    private String commentId;
    private String userId;
    private String comment;
    private String commentBy;
    private String imgID;
    private String commentTime;
}
