package com.example.myApp.demos.service;

import com.example.myApp.demos.dto.PageDto;
import com.example.myApp.demos.entity.MsgEntity;
import com.github.pagehelper.PageInfo;

import javax.servlet.http.HttpServletRequest;


public interface LogService {

    PageInfo<MsgEntity> getMsg(PageDto dto , HttpServletRequest request);

}
