package com.example.myApp.demos.mq;

import com.example.myApp.demos.entity.LogEvent;
import com.example.myApp.demos.service.LogEventService;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@RocketMQMessageListener(
        topic = "async-log-topic",
        consumerGroup = "async-log-consumer",
        messageModel = MessageModel.CLUSTERING
)
public class LogConsumer implements RocketMQListener<LogEvent> {
    @Resource
    private LogEventService logEventService;

    @Override
    public void onMessage(LogEvent event) {
        System.out.println("消费成功 保存到log_event表");
        logEventService.saveLogEvent(event);
    }
}
