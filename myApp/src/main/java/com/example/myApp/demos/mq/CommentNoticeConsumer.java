package com.example.myApp.demos.mq;

import com.example.myApp.demos.Constants;
import com.example.myApp.demos.dto.CommentNotice;
import com.example.myApp.demos.entity.MsgEntity;
import com.example.myApp.demos.mapper.LogMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;

@Component
@RocketMQMessageListener(
        topic = Constants.COMMENT_NOTICE_TOPIC,
        consumerGroup = Constants.COMMENT_CONSUMER_GROUP,
        selectorExpression = "COMMENT_NOTICE_TAG"
)
@Slf4j
public class CommentNoticeConsumer implements RocketMQListener<CommentNotice> {

    @Resource
    private LogMapper logMapper;

    @Override
    public void onMessage(CommentNotice notice) {
        if (notice == null) {
            log.error("消费评论失败：消息体不能为空");
        }

        String imgID = Objects.requireNonNull(notice).getImgID();
        String commentBy = notice.getCommentBy();
        String msgId =notice.getCommentId();

        try {
            MsgEntity msgEntity = new MsgEntity();
            msgEntity.setMsgId(imgID + msgId);
            msgEntity.setMsgContent(notice.getComment());
            msgEntity.setMsgType("1");
            msgEntity.setUpdateTime(notice.getCommentTime());
            msgEntity.setGroupId(Constants.COMMENT_CONSUMER_GROUP);
            msgEntity.setUserId(notice.getUserId());
            msgEntity.setCreateBy(commentBy);
            logMapper.addLog(msgEntity);
            log.info("评论通知消息处理成功：msgId={}", msgId);
        } catch (Exception e) {
            //异常处理：记录详细日志，便于排查问题
            log.error("消费评论通知失败：msgId={}", msgId, e);
            // 可选：如果需要重试，可以抛出异常（RocketMQ会根据重试策略重试）
            throw new RuntimeException("评论通知入库失败", e);
        }
    }
}
