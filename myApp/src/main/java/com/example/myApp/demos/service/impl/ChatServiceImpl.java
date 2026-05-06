package com.example.myApp.demos.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.myApp.demos.Constants;
import com.example.myApp.demos.dto.ContentItem;
import com.example.myApp.demos.dto.DrawDto;
import com.example.myApp.demos.dto.ImageUrl;
import com.example.myApp.demos.entity.UserChat;
import com.example.myApp.demos.service.ChatService;
import com.example.myApp.demos.service.FileOptService;
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
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

    @Value("${db.draw}")
    private String draw;

    @Value("${db.link}")
    private String link;

    @Resource
    private LogService logService;

    @Resource
    private FileOptService fileOptService;

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
        logService.saveUserChat(new UserChat(chatId, uid, msg, "", "TALK"));

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
        if (StringUtils.isAnyEmpty(uid)) throw new RuntimeException(Constants.TOKEN_EXPIRED);
        if (StringUtils.isEmpty(tmp)) tmp = sdf.format(new Date());
        return logService.getChat(tmp, uid);
    }

    @Override
    public String draw(String msg, MultipartFile[] files, String uid) throws Exception {
        if (StringUtils.isAnyEmpty(uid, msg)) {
            throw new RuntimeException(Constants.PARAM_ERR);
        }

        String chatId = RandomUtil.randomNumbers(18);
        log.info("发起绘图 chatId:{}，用户:{}，输入:{}", chatId, uid, msg);


        List<ContentItem> items = new ArrayList<>();
        items.add(new ContentItem("text", msg));
        for (MultipartFile file : files) {
            String url = fileOptService.uploadAvatarFile(file, "draw");
            ContentItem item = new ContentItem("image_url", new ImageUrl(url));
            items.add(item);
        }

        int seed = RandomUtil.randomInt(6);
        DrawDto drawDto = new DrawDto(draw, seed, items);
        String body = JSON.toJSONString(drawDto);

        HttpResponse response = HttpRequest.post(link)
                .header(Header.CONTENT_TYPE, "application/json")
                .header(Header.AUTHORIZATION, "Bearer " + apiKey)
                .body(body)
                .execute();

        String taskId = getTaskId(response.body());
        logService.saveUserChat(new UserChat(chatId, uid, msg, "", taskId, "OTHER"));
        log.info("模型处理中，任务id:{}", taskId);
        return taskId;
    }

    private String getTaskId(String jsonStr) {
        JSONObject jsonObject = JSON.parseObject(jsonStr);
        return jsonObject.getString("id");
    }

    @Override
    public String query(String taskId) {
        String url = "https://ark.cn-beijing.volces.com/api/v3/contents/generations/tasks/" + taskId;
        HttpResponse response = HttpRequest.get(url).header(Header.CONTENT_TYPE, "application/json")
                .header(Header.AUTHORIZATION, "Bearer " + apiKey)
                .execute();
        JSONObject jso = JSON.parseObject(response.body());
        String status = jso.getString("status");
        if("succeeded".equals(status)) {
            String fileUrl = jso.getJSONObject("content").getString("file_url");
            logService.modifyUserChat(taskId ,fileUrl);
        }
        return status;
    }
}
