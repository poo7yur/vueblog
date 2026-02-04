package com.example.myApp.demos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookDto {

    private int pageSize=10;
    private int pageNo=1;
    private String userId;
    private String userName;
}
