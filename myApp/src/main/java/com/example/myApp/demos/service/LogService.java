package com.example.myApp.demos.service;

import com.example.myApp.demos.dto.PageDto;
import com.example.myApp.demos.entity.MsgEntity;
import com.example.myApp.demos.vo.SongVo;
import com.github.pagehelper.PageInfo;

import javax.servlet.http.HttpServletRequest;
import java.util.List;


public interface LogService {

    PageInfo<MsgEntity> getMsg(PageDto dto , HttpServletRequest request);

    List<SongVo> listSongs(String uid);
}
