package com.example.myApp.demos.service;

import com.example.myApp.demos.dto.ChapterDto;
import com.example.myApp.demos.dto.ImageDto;
import com.example.myApp.demos.vo.ChapterDataVo;
import com.example.myApp.demos.vo.PageImageVo;

public interface BookService {

    ChapterDataVo loadChapter(ChapterDto dto);

    PageImageVo listBooks(ImageDto dto);

}
