package com.example.myApp.demos.service.impl;

import com.example.myApp.demos.Constants;
import com.example.myApp.demos.entity.MsgEntity;
import com.example.myApp.demos.mapper.LogMapper;
import com.example.myApp.demos.service.LogService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;

@Service
public class LogServiceImpl implements LogService {

    @Resource
    private LogMapper logMapper;

    @Override
    public List<MsgEntity> getMsg(String userId) {
        //根据用户id查询订阅的消息组id
        Set<String> groupIds = logMapper.getMsgGroupIds(userId);
        if(groupIds.isEmpty()) throw new RuntimeException(Constants.MSG_NOT_SUBSCRIBE);
        return logMapper.getMsg(groupIds);
    }
}
