package com.example.myApp.demos.service;

import com.example.myApp.demos.dto.EssayDto;
import com.example.myApp.demos.dto.PageDto;
import com.example.myApp.demos.entity.Essay;
import com.example.myApp.demos.mapper.EssayMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class EssayServiceImpl implements EssayService {

    @Resource
    private EssayMapper essayMapper;

    @Resource
    private FileOptService fileOptService;

    @Override
    public PageInfo<Essay> queryEssay(PageDto dto) {
        PageHelper.startPage(dto.getPageNum(), dto.getPageSize());
        Integer share = 1;
        if (!StringUtils.isEmpty(dto.getUserId())) share = null;
        List<Essay> list = essayMapper.queryEssay(dto, share);
        return new PageInfo<>(list);
    }

    @Override
    public String publishEssay(EssayDto essayDto) {
        essayMapper.updateShare(essayDto.getId(), essayDto.getStatus());
        return "发布成功";
    }

    @Override
    public String deleteEssay(EssayDto essayDto) {
        essayMapper.delEssayById(essayDto.getId());
        return "删除成功";
    }

    @Override
    public String saveEssayContent(EssayDto essayDto) throws Exception {
        String htmlContent = essayDto.getContent();
        String storagePath = essayDto.getStoragePath();
        fileOptService.uploadHtmlFile(storagePath ,htmlContent);
        return "保存成功";
    }
}
