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
    private int status;
    private int isShare;
    private String htmlContent;
}
