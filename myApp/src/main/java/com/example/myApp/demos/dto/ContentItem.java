package com.example.myApp.demos.dto;

import lombok.Data;

@Data
public class ContentItem {
    private String type;
    private String text;
    private ImageUrl image_url;

    public ContentItem(String type, String text) {
        this.type = type;
        this.text = text;
    }

    public ContentItem(String type, ImageUrl image_url) {
        this.type = type;
        this.image_url = image_url;
    }
}
