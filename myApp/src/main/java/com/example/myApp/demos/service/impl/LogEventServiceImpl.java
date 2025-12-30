package com.example.myApp.demos.service.impl;

import com.example.myApp.demos.entity.LogEvent;
import com.example.myApp.demos.mapper.LogEventMapper;
import com.example.myApp.demos.service.LogEventService;

import javax.annotation.Resource;

public class LogEventServiceImpl implements LogEventService {
    @Resource
    private LogEventMapper logEventMapper;

    @Override
    public void saveLogEvent(LogEvent event) {
        logEventMapper.saveLogEvent(event);
    }
}
