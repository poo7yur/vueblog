package com.example.myApp.demos.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LogEntity {

    private String module;
    private String description;
    private String ip;
    private String loginUser;
    private String uri;
    private String method;
    private String params;
    private Object result;
    private Long accessTime;
}
