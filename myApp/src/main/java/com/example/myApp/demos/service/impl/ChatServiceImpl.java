package com.example.myApp.demos.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.example.myApp.demos.Constants;
import com.example.myApp.demos.entity.UserChat;
import com.example.myApp.demos.service.ChatService;
import com.example.myApp.demos.service.LogService;
import com.example.myApp.demos.vo.ChatLogVo;
import com.example.myApp.demos.vo.ChatResponse;
import com.volcengine.ark.runtime.model.responses.content.OutputContentItemText;
import com.volcengine.ark.runtime.model.responses.content.ReasoningSummaryPart;
import com.volcengine.ark.runtime.model.responses.item.BaseItem;
import com.volcengine.ark.runtime.model.responses.item.ItemOutputMessage;
import com.volcengine.ark.runtime.model.responses.item.ItemReasoning;
import com.volcengine.ark.runtime.model.responses.request.CreateResponsesRequest;
import com.volcengine.ark.runtime.model.responses.request.ResponsesInput;
import com.volcengine.ark.runtime.model.responses.response.ResponseObject;
import com.volcengine.ark.runtime.service.ArkService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


@Service
@Slf4j
public class ChatServiceImpl implements ChatService {

    @Value("${db.key}")
    private String apiKey;

    @Value("${db.url}")
    private String url;

    @Value("${db.model}")
    private String model;

    @Resource
    private LogService logService;

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public ChatResponse chat(String msg, String uid) {
        // 输入校验（必须加）
        if (StringUtils.isAnyEmpty(msg, uid)) {
            log.warn("输入参数非法，uid:{}，msg:{}", uid, msg);
            return new ChatResponse(null, "", "输入内容不能为空", "fail");
        }

        String chatId = RandomUtil.randomNumbers(18);
        log.info("发起对话 chatId:{}，用户:{}，输入:{}", chatId, uid, msg);

        // 初始状态落库
        logService.saveUserChat(new UserChat(chatId, uid, msg, ""));

        // 提前定义变量，finally 能拿到最终状态
        String thinkContent = "";
        String summaryContent = "";
        String answerStatus = "fail"; // 默认失败
        ArkService arkService = null;

        try {
            arkService = ArkService.builder()
                    .apiKey(apiKey)
                    .baseUrl(url)
                    .build();

            CreateResponsesRequest request = CreateResponsesRequest.builder()
                    .model(model)
                    .input(ResponsesInput.builder().stringValue(msg).build())
                    .build();

            ResponseObject resp = arkService.createResponse(request);

            List<BaseItem> output = resp.getOutput();

            for (BaseItem item : output) {
                // 1. 提取思考内容
                if (item instanceof ItemReasoning) {
                    ItemReasoning reasoning = (ItemReasoning) item;
                    thinkContent = reasoning.getSummary().stream()
                            .filter(part -> "summary_text".equals(part.getType()))
                            .map(ReasoningSummaryPart::getText)
                            .collect(Collectors.joining("\n"));
                }

                // 2. 提取最终回答
                if (item instanceof ItemOutputMessage) {
                    ItemOutputMessage message = (ItemOutputMessage) item;
                    summaryContent = message.getContent().stream()
                            .filter(contentItem -> contentItem instanceof OutputContentItemText)
                            .map(contentItem -> (OutputContentItemText) contentItem)
                            .map(OutputContentItemText::getText)
                            .collect(Collectors.joining("\n"));
                }
            }

            answerStatus = "success";
            return new ChatResponse(chatId, thinkContent, summaryContent, answerStatus);

        } catch (Exception e) {
            log.error("调用AI接口异常 chatId:{}", chatId, e);
            answerStatus = "error";
            summaryContent = "服务调用失败，请稍后重试";
            return new ChatResponse(chatId, "", summaryContent, answerStatus);
        } finally {
            // 最终日志更新（此时 answerStatus 一定是最终状态）
            logService.updateChatLog(chatId, summaryContent, answerStatus);
            // 安全关闭线程池
            if (arkService != null) {
                try {
                    arkService.shutdownExecutor();
                } catch (Exception ex) {
                    log.error("关闭arkService失败 chatId:{}", chatId, ex);
                }
            }
        }
    }

    @Override
    public ChatLogVo logs(String tmp, String uid) {
        if(StringUtils.isAnyEmpty(uid)) throw new RuntimeException(Constants.TOKEN_EXPIRED);
        if(StringUtils.isEmpty(tmp)) tmp = sdf.format(new Date());
        return logService.getChat(tmp, uid);
    }
}
