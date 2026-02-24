package com.example.myApp.demos.dto;

import lombok.Data;

@Data
public class PayDto {
    private String userId;
    private String targetSpace;
    private Double amount;
}
