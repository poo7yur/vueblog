package com.example.myApp.demos.service.impl;

import com.example.myApp.demos.Constants;
import com.example.myApp.demos.entity.LogEvent;
import com.example.myApp.demos.entity.MsgEntity;
import com.example.myApp.demos.mapper.LogEventMapper;
import com.example.myApp.demos.service.LogEventService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;

@Service
public class LogEventServiceImpl implements LogEventService {

    @Resource
    private LogEventMapper logEventMapper;

    @Override
    public void saveLogEvent(LogEvent event) {
        logEventMapper.saveLogEvent(event);
    }

    @Override
    public List<MsgEntity> getMsg(String userId) {
        //根据用户id查询订阅的消息组id
        Set<String> groupIds = logEventMapper.getMsgGroupIds(userId);
        if(groupIds.isEmpty()) throw new RuntimeException(Constants.MSG_NOT_SUBSCRIBE);
        return logEventMapper.getMsg(groupIds);
    }
}
