package com.example.myApp.demos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActDto {
    private String fromUser;//操作人
    private String toUser;//操作对象
    private String action;//关注+取消关注+拉黑+解除拉黑
}
