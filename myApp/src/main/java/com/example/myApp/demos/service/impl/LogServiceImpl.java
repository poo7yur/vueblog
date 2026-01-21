package com.example.myApp.demos.service.impl;

import com.example.myApp.demos.Constants;
import com.example.myApp.demos.dto.PageDto;
import com.example.myApp.demos.entity.MsgEntity;
import com.example.myApp.demos.mapper.LogMapper;
import com.example.myApp.demos.service.LogService;
import com.example.myApp.demos.util.JwtUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Service
public class LogServiceImpl implements LogService {

    @Resource
    private LogMapper logMapper;

    @Override
    public PageInfo<MsgEntity> getMsg(PageDto dto, HttpServletRequest request) {
        // 0. 判断token是否失效
        Claims claims = JwtUtil.parseToken(request.getHeader("token"));
        if (claims.getExpiration() != null && claims.getExpiration().before(new Date())) {
            throw new RuntimeException(Constants.TOKEN_EXPIRED);
        }
        String userId = claims.get("userId").toString();
        // 1. 查询订阅的消息组ID
        Set<String> groupIds = logMapper.getMsgGroupIds(userId);

        // 2. 校验：如果没有订阅任何消息组，抛出异常
        if (groupIds.isEmpty()) throw new RuntimeException(Constants.MSG_NOT_SUBSCRIBE);

        // 3. 初始化分页参数（PageHelper会自动拦截后续的MyBatis查询并添加分页条件）
        PageHelper.startPage(dto.getPageNum(), dto.getPageSize());

        // 4. 核心：根据消息组ID集合分页查询消息列表
        List<MsgEntity> msgList = logMapper.getMsg(groupIds, dto.getKeyword(), userId);
        return new PageInfo<>(msgList);
    }
}
