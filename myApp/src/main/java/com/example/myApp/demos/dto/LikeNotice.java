package com.example.myApp.demos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LikeNotice implements Serializable {

   private String imgID;
   private String imgName;
   private String likerId;
   private String ownerId;
   private String likeTime;
}
