package com.example.myApp.demos.mapper;

import com.example.myApp.demos.entity.ShareImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ImageMapper {

    void shareImg(@Param("si") ShareImage shareImage);
}
