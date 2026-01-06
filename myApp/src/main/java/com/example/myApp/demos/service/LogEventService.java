package com.example.myApp.demos.service;

import com.example.myApp.demos.entity.LogEvent;
import com.example.myApp.demos.entity.MsgEntity;

import java.util.List;

public interface LogEventService {
    void saveLogEvent(LogEvent event);

    List<MsgEntity> getMsg(String userId);

}
