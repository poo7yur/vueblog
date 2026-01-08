package com.example.myApp.demos.service;

import com.example.myApp.demos.entity.MsgEntity;

import java.util.List;

public interface LogService {

    List<MsgEntity> getMsg(String userId);

}
