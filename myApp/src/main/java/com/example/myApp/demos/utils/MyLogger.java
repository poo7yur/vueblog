package com.example.myApp.demos.utils;

import com.example.myApp.demos.entity.LogEvent;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class MyLogger {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    private final String topic = "async-log-topic";

    public void log(String level ,String msg){
        LogEvent logEvent = new LogEvent(msg, level, System.currentTimeMillis());

        rocketMQTemplate.asyncSend(topic, logEvent, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                System.out.println("记录成功");
            }

            @Override
            public void onException(Throwable throwable) {
                //失败 保存到本地
            }
        });
    }
}
