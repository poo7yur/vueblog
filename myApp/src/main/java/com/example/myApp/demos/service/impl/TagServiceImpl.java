package com.example.myApp.demos.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.example.myApp.demos.Constants;
import com.example.myApp.demos.dto.TagDto;
import com.example.myApp.demos.entity.Tag;
import com.example.myApp.demos.mapper.TagMapper;
import com.example.myApp.demos.service.TagService;
import com.example.myApp.demos.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@Service
public class TagServiceImpl implements TagService {

    @Resource
    private TagMapper tagMapper;

    @Resource
    private UserService userService;

    @Override
    public List<Tag> listTags(String uid) {
        boolean isAdmin = userService.checkAdminRole(uid);
        String isAdminStr = isAdmin ? "1" : "0";
        return tagMapper.listTags(uid, isAdminStr);
    }

    @Override
    public String addTag(Tag tag) {
        if (StringUtils.isEmpty(tag.getTagName()) || StringUtils.isEmpty(tag.getUpdateBy()))
            throw new RuntimeException(Constants.PARAM_ERR);
        boolean isAdmin = userService.checkAdminRole(tag.getUpdateBy());
        if (isAdmin) tag.setIsPublic(1);
        if (StringUtils.isEmpty(tag.getColor())) tag.setColor("#999999");
        tag.setTagId(RandomUtil.randomNumbers(12));
        tag.setUpdateTime(new Date());
        tagMapper.addTag(tag);
        return "保存成功";
    }

    @Override
    public String updateTag(Tag tag) {
        if (StringUtils.isEmpty(tag.getTagId()) || StringUtils.isEmpty(tag.getUpdateBy()))
            throw new RuntimeException(Constants.PARAM_ERR);
        tagMapper.updateTag(tag);
        return "修改成功";
    }

    @Override
    public String deleteTag(String tagId, String uid) {
        if (StringUtils.isEmpty(tagId) || StringUtils.isEmpty(uid)) throw new RuntimeException(Constants.PARAM_ERR);
        boolean isAdmin = userService.checkAdminRole(uid);
        if(isAdmin) uid = null;
        tagMapper.deleteTag(tagId, uid);
        return "删除成功";
    }

    @Override
    public String bindTag(TagDto tagDto) {
        tagMapper.bindTag(tagDto);
        return "绑定成功";
    }
}
