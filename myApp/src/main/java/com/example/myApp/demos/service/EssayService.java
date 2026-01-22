package com.example.myApp.demos.service;

import com.example.myApp.demos.dto.EssayDto;
import com.example.myApp.demos.dto.LinkDto;
import com.example.myApp.demos.dto.PageDto;
import com.example.myApp.demos.entity.Essay;
import com.github.pagehelper.PageInfo;

import javax.servlet.http.HttpServletRequest;


public interface EssayService {
    PageInfo<Essay> queryEssay(PageDto dto);

    String publishEssay(EssayDto essayDto);

    String deleteEssay(EssayDto essayDto);

    String saveEssayContent(EssayDto essayDto) throws Exception;

    String createEssay(EssayDto essayDto , HttpServletRequest request) throws Exception;

    String saveLink(LinkDto dto) throws Exception;
}
