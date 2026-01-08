package com.example.myApp.demos.mq;

import com.alibaba.fastjson.JSONObject;
import com.example.myApp.demos.Constants;
import com.example.myApp.demos.entity.LogEntity;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(
        topic = Constants.ACCESS_LOG_TOPIC,
        consumerGroup = "access-log-consumer",
        messageModel = MessageModel.CLUSTERING
)
public class AccessLogConsumer implements RocketMQListener<LogEntity> {
    @Override
    public void onMessage(LogEntity logEntity) {
        System.out.println("接口请求日志消费成功：" + JSONObject.toJSONString(logEntity));
    }
}
