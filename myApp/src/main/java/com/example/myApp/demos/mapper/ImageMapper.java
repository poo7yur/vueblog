package com.example.myApp.demos.mapper;

import com.example.myApp.demos.entity.CommentEntity;
import com.example.myApp.demos.entity.ShareImage;
import com.example.myApp.demos.entity.UserImgRel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ImageMapper {

    void shareImg(@Param("si") ShareImage shareImage);

    void recordUserImgRel(@Param("t") UserImgRel userImgRel);

    String queryOwnerId(String imgID);

    Integer checkUserImgRelExists(@Param("id") String imgID, @Param("uid") String userId);

    void insertComment(@Param("c") CommentEntity comment);

    List<CommentEntity> queryComment(String id);

}
