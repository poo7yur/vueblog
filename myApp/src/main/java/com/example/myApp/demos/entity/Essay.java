package com.example.myApp.demos.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Essay {

    private String id;
    private String summary;
    private String title;
    private String storagePath;
    private String category;
    private Date updateTime;
    private String createUser;
    private int status;
    private int isPublic;
    private int isShare;
    private int type;

    public Essay(String id, String title, String summary ,int isPublic) {
        this.id = id;
        this.title = title;
        this.summary = summary;
        this.isPublic = isPublic;
    }
}
