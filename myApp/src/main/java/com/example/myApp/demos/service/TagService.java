package com.example.myApp.demos.service;

import com.example.myApp.demos.dto.TagDto;
import com.example.myApp.demos.entity.Tag;

import java.util.List;

public interface TagService {
    List<Tag> listTags(String uid);

    String addTag(Tag tag);

    String updateTag(Tag tag);

    String deleteTag(String tagId, String uid);

    String bindTag(TagDto dto);

}
