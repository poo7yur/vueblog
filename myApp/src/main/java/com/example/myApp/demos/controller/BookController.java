package com.example.myApp.demos.controller;

import com.example.myApp.demos.aop.AccessLog;
import com.example.myApp.demos.dto.BookDto;
import com.example.myApp.demos.dto.ChapterDto;
import com.example.myApp.demos.entity.R;
import com.example.myApp.demos.service.BookService;
import com.example.myApp.demos.util.JwtUtil;
import com.example.myApp.demos.vo.ChapterDataVo;
import com.example.myApp.demos.vo.PageImageVo;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
public class BookController {

    @Resource
    private BookService bookService;

    @PostMapping("/listBooks")
    public R<PageImageVo> listBooks(@RequestBody BookDto dto , HttpServletRequest request) {
        try {
            dto.setUserId(JwtUtil.parseUid(request));
            return R.ok(bookService.listBooks(dto));
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @GetMapping("/delBook")
    @AccessLog(module = "book manage" ,description = "delete book")
    public R<String> delBook(@RequestParam("id") String id , HttpServletRequest request) {
        try {
            String userId = JwtUtil.parseUid(request);
            return R.ok(bookService.delBook(id ,userId));
        } catch (Exception e){
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/uploadBook")
    public R<String> uploadBook(@RequestParam("file") MultipartFile file , HttpServletRequest request) {
        try {
            String userId = JwtUtil.parseUid(request);
            return R.ok(bookService.uploadBook(file ,userId));
        } catch (Exception e){
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
