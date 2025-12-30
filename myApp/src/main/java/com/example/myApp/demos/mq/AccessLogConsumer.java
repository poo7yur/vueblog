package com.example.myApp.demos.mq;

import com.alibaba.fastjson.JSONObject;
import com.example.myApp.demos.entity.LogEntity;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(
        consumerGroup = "access-log-consumer",
        topic = "access-log-topic",
        messageModel = MessageModel.CLUSTERING
)
public class AccessLogConsumer implements RocketMQListener<LogEntity> {
    @Override
    public void onMessage(LogEntity logEntity) {
        //写数据库
        System.out.println("访问日志保存成功：" + JSONObject.toJSONString(logEntity));
    }
}
