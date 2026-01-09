package com.example.myApp.demos.mq;

import cn.hutool.core.util.RandomUtil;
import com.example.myApp.demos.Constants;
import com.example.myApp.demos.dto.LikeNotice;
import com.example.myApp.demos.entity.MsgEntity;
import com.example.myApp.demos.mapper.LogMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;


@Component
@RocketMQMessageListener(
        topic = Constants.IMAGE_LIKE_NOTICE_TOPIC,  // 与生产者的主题一致
        consumerGroup = "IMAGE_LIKE_CONSUMER_GROUP",  // 消费者组（自定义，需唯一）
        selectorExpression = "IMAGE_LIKE_TAG"  // 消息标签（与生产者一致，空则消费所有标签）
)
@Slf4j
public class LikeNoticeConsumer implements RocketMQListener<LikeNotice> {

    @Resource
    private LogMapper logMapper;

    @Override
    public void onMessage(LikeNotice likeNotice) {
        // 入参校验：防止空消息或关键字段为空导致入库异常
        if (likeNotice == null) {
            log.error("消费点赞通知消息失败：消息体为空");
            return;
        }
        String imgID = likeNotice.getImgID();
        String likerId = likeNotice.getLikerId();

        try {
            //写到t_msg表
            MsgEntity msgEntity = new MsgEntity();
            msgEntity.setMsgId(imgID + RandomUtil.randomNumbers(10));
            msgEntity.setMsgContent("图片：" + likeNotice.getImgName() + " 被点赞了");
            msgEntity.setMsgType(0);
            msgEntity.setUpdateTime(likeNotice.getLikeTime());
            msgEntity.setGroupId("IMAGE_LIKE_CONSUMER_GROUP");
            msgEntity.setMsgType(0);
            msgEntity.setUserId(likeNotice.getOwnerId());
            msgEntity.setCreateBy(likerId);
            logMapper.addLog(msgEntity);
            log.info("点赞通知消息处理成功：imgID={}, likerId={}", imgID, likerId);
        } catch (Exception e) {
            //异常处理：记录详细日志，便于排查问题
            log.error("消费点赞通知消息失败：入库UserImgRel表异常，imgID={}, likerId={}", imgID, likerId, e);
            // 可选：如果需要重试，可以抛出异常（RocketMQ会根据重试策略重试）
            throw new RuntimeException("点赞关系入库失败", e);
        }
    }
}
