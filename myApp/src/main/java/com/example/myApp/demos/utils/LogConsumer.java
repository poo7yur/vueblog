package com.example.myApp.demos.utils;

import com.example.myApp.demos.entity.LogEvent;
import com.example.myApp.demos.mapper.LogEventMapper;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@RocketMQMessageListener(
        topic = "async-log-topic",
        consumerGroup = "async-log-consumer",
        messageModel = MessageModel.CLUSTERING,
        maxReconsumeTimes = 3
)
public class LogConsumer implements RocketMQListener<LogEvent> {
    @Resource
    private LogEventMapper logEventMapper;

    @Override
    public void onMessage(LogEvent event) {
        //消费成功 写数据库
        logEventMapper.saveLogEvent(event);
    }
}
