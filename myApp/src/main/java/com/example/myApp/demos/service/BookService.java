package com.example.myApp.demos.service;

import com.example.myApp.demos.dto.BookDto;
import com.example.myApp.demos.dto.ChapterDto;
import com.example.myApp.demos.vo.ChapterDataVo;
import com.example.myApp.demos.vo.PageImageVo;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


public interface BookService {

    ChapterDataVo loadChapter(ChapterDto dto);

    PageImageVo listBooks(BookDto dto);

    String delBook(String id, String userId);

    String uploadBook(MultipartFile file, String userId) throws IOException;

}
