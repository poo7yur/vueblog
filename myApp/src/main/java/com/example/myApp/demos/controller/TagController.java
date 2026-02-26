package com.example.myApp.demos.controller;

import com.example.myApp.demos.dto.TagDto;
import com.example.myApp.demos.entity.R;
import com.example.myApp.demos.entity.Tag;
import com.example.myApp.demos.service.TagService;
import com.example.myApp.demos.util.JwtUtil;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
public class TagController {

    @Resource
    private TagService tagService;

    @GetMapping("/listTags")
    public R<List<Tag>> listTags(HttpServletRequest request) {
        String uid = JwtUtil.parseUid(request);
        return R.ok(tagService.listTags(uid));
    }

    @PostMapping("/addTag")
    public R<String> addTag(@RequestBody Tag tag, HttpServletRequest request) {
        try {
            String uid = JwtUtil.parseUid(request);
            tag.setUpdateBy(uid);
            return R.ok(tagService.addTag(tag));
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/updateTag")
    public R<String> updateTag(@RequestBody Tag tag, HttpServletRequest request) {
        try {
            String uid = JwtUtil.parseUid(request);
            tag.setUpdateBy(uid);
            return R.ok(tagService.updateTag(tag));
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @GetMapping("/deleteTag")
    public R<String> deleteTag(@RequestParam String tagId, HttpServletRequest request) {
        try {
            String uid = JwtUtil.parseUid(request);
            return R.ok(tagService.deleteTag(tagId, uid));
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/bindTag")
    public R<String> bindTag(@RequestBody TagDto tagDto) {
        try {
            return R.ok(tagService.bindTag(tagDto));
        } catch (Exception e){
            return R.fail(e.getMessage());
        }
    }

}
