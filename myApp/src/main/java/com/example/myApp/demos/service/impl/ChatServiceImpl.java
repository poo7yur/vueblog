package com.example.myApp.demos.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.example.myApp.demos.service.ChatService;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

    @Override
    public ChatResponse chat(String msg) {
        String chatId = RandomUtil.randomNumbers(18);
        log.info("发起豆包对话 chatId:{}，用户输入:{}", chatId, msg);

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

            // ===================== 修复后的核心解析 =====================
            String thinkContent = "";
            String summaryContent = "";

            List<BaseItem> output = resp.getOutput();
            if (output != null && !output.isEmpty()) {
                for (BaseItem item : output) {
                    // 1. 提取思考内容（thinking）
                    if (item instanceof ItemReasoning) {
                        ItemReasoning reasoning = (ItemReasoning) item;
                        thinkContent = reasoning.getSummary().stream()
                                .filter(part -> "summary_text".equals(part.getType()))
                                .map(ReasoningSummaryPart::getText)
                                .collect(Collectors.joining("\n"));
                    }

                    // 2. 提取最终回答（修复类型转换问题）
                    if (item instanceof ItemOutputMessage) {
                        ItemOutputMessage message = (ItemOutputMessage) item;
                        summaryContent = message.getContent().stream()
                                .filter(contentItem -> contentItem instanceof OutputContentItemText)
                                .map(contentItem -> (OutputContentItemText) contentItem)
                                .map(OutputContentItemText::getText)
                                .collect(Collectors.joining("\n"));
                    }
                }
            }

            log.info("对话完成 chatId:{}，思考内容：{}", chatId, thinkContent);
            return new ChatResponse(chatId, thinkContent, summaryContent, "success");

        } catch (Exception e) {
            log.error("调用豆包API异常 chatId:{}", chatId, e);
            return new ChatResponse(chatId, "", "", "error");
        } finally {
            if (arkService != null) {
                arkService.shutdownExecutor();
            }
        }
    }
}
