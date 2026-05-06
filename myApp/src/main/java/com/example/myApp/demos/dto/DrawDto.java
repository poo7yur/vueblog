package com.example.myApp.demos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DrawDto {

    private String model;
    private int seed;
    private List<ContentItem> content;
}
