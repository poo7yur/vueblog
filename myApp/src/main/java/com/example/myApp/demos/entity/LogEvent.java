package com.example.myApp.demos.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LogEvent implements Serializable {

    private String message;
    private String level;
    private long timestp;
}
