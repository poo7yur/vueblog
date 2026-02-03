package com.example.myApp.demos.controller;

import com.example.myApp.demos.dto.ChapterDto;
import com.example.myApp.demos.dto.ImageDto;
import com.example.myApp.demos.entity.R;
import com.example.myApp.demos.service.BookService;
import com.example.myApp.demos.util.JwtUtil;
import com.example.myApp.demos.vo.ChapterDataVo;
import com.example.myApp.demos.vo.PageImageVo;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
public class BookController {

    @Resource
    private BookService bookService;

    @PostMapping("/listBooks")
    public R<PageImageVo> listBooks(@RequestBody ImageDto dto , HttpServletRequest request) {
        try {
            dto.setUserId(JwtUtil.parseUid(request));
            return R.ok(bookService.listBooks(dto));
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/loadChapter")
    public R<ChapterDataVo> loadChapter(@RequestBody ChapterDto dto) {
        try {
            return R.ok(bookService.loadChapter(dto));
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }
}
