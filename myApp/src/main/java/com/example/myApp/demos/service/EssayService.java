package com.example.myApp.demos.service;

import com.example.myApp.demos.dto.EssayDto;
import com.example.myApp.demos.dto.PageDto;
import com.example.myApp.demos.entity.Essay;
import com.github.pagehelper.PageInfo;


public interface EssayService {
    PageInfo<Essay> queryEssay(PageDto dto);

    String publishEssay(EssayDto essayDto);

    String deleteEssay(EssayDto essayDto);

    String saveEssayContent(EssayDto essayDto) throws Exception;
}
