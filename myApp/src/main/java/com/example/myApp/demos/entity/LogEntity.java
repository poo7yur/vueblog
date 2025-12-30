package com.example.myApp.demos.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

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
    private Map<String, Object> params;
    private Object result;
    private Long accessTime;
}
