package com.example.myApp.demos.mapper;

import com.example.myApp.demos.dto.TagDto;
import com.example.myApp.demos.entity.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TagMapper {
    List<Tag> listTags(@Param("uid") String uid, @Param("isAdmin") String isAdmin);

    void addTag(@Param("t") Tag tag);

    void updateTag(Tag tag);

    void deleteTag(@Param("tid") String tagId, @Param("uid") String uid);

    void bindTag(@Param("dto") TagDto dto);

}
