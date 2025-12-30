package com.example.myApp.demos.entity;

import lombok.Data;

@Data
public class OrderDto {
    private String orderId;
    private String userId;
    private String platform;
}
