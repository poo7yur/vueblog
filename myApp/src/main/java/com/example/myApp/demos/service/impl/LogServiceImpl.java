package com.example.myApp.demos.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.example.myApp.demos.Constants;
import com.example.myApp.demos.config.PayConfig;
import com.example.myApp.demos.dto.PageDto;
import com.example.myApp.demos.entity.MsgEntity;
import com.example.myApp.demos.entity.Order;
import com.example.myApp.demos.entity.UserChat;
import com.example.myApp.demos.mapper.LogMapper;
import com.example.myApp.demos.mapper.UserMapper;
import com.example.myApp.demos.service.LogService;
import com.example.myApp.demos.util.DirUtil;
import com.example.myApp.demos.util.JwtUtil;
import com.example.myApp.demos.vo.ChatLogVo;
import com.example.myApp.demos.vo.SongVo;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.*;

@Service
@Slf4j
public class LogServiceImpl implements LogService {

    @Value("${music.store}")
    private String musicStore;

    @Resource
    private LogMapper logMapper;

    @Resource
    private PayConfig payConfig;

    @Resource
    private UserMapper userMapper;

    @Override
    public PageInfo<MsgEntity> getMsg(PageDto dto, HttpServletRequest request) {
        // 0. 判断token是否失效
        Claims claims = JwtUtil.parseToken(request.getHeader("token"));
        if (claims.getExpiration() != null && claims.getExpiration().before(new Date())) {
            throw new RuntimeException(Constants.TOKEN_EXPIRED);
        }
        String userId = claims.get("userId").toString();
        // 1. 查询订阅的消息组ID
        Set<String> groupIds = logMapper.getMsgGroupIds(userId);

        // 2. 校验：如果没有订阅任何消息组，抛出异常
        if (groupIds.isEmpty()) throw new RuntimeException(Constants.MSG_NOT_SUBSCRIBE);

        // 3. 初始化分页参数（PageHelper会自动拦截后续的MyBatis查询并添加分页条件）
        PageHelper.startPage(dto.getPageNum(), dto.getPageSize());

        // 4. 核心：根据消息组ID集合分页查询消息列表
        List<MsgEntity> msgList = logMapper.getMsg(groupIds, dto.getKeyword(), userId);
        return new PageInfo<>(msgList);
    }

    @Override
    public List<SongVo> listSongs(String uid) {
        // 1. 校验 uid 合法性
        if (StringUtils.isEmpty(uid)) {
            throw new RuntimeException(Constants.TOKEN_EXPIRED);
        }

        // 2. 初始化变量
        List<SongVo> songVos = new ArrayList<>();
        String endFix = ".mp3"; // 可考虑提取为常量或配置项
        File musicStoreDir = new File(musicStore);

        try {
            // 3. 扫描音乐文件
            List<String> songsUrls = DirUtil.scanBookFile(musicStoreDir, true, endFix);

            // 4. 处理每个文件路径
            for (String song : songsUrls) {
                // 统一处理：将 Windows 反斜杠替换为正斜杠，简化后续处理
                String normalizedPath = song.replace("\\", "/");
                int lastSlashIndex = normalizedPath.lastIndexOf("/");
                if (lastSlashIndex == -1) {
                    // 如果未找到 '/'，跳过当前文件（避免越界）
                    continue;
                }

                String fullName = normalizedPath.substring(lastSlashIndex + 1); // 提取文件名部分
                String name = fullName.replace(endFix, ""); // 去除后缀

                // 添加到结果列表
                songVos.add(new SongVo(song, name));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while scanning music files", e);
        }

        return songVos;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String alipayNotify(Map<String, String> params) throws AlipayApiException {
        boolean signVerified = AlipaySignature.rsaCheckV1(
                params,
                payConfig.getPublicKey(),
                "UTF-8",
                "RSA2"
        );
        if (!signVerified) {
            log.error("支付宝回调签名验证失败");
            return "fail";
        }

        String outTradeNo = params.get("out_trade_no");  // 商户订单号
        String tradeStatus = params.get("trade_status"); // 交易状态
        String tradeNo = params.get("trade_no");         // 支付宝交易号

        log.info("收到支付宝回调, orderId: {}, status: {}", outTradeNo, tradeStatus);
        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            // 幂等性检查：查询订单是否已处理
            Order order = userMapper.getOrderById(outTradeNo);
            if (order == null) {
                log.error("订单不存在: {}", outTradeNo);
                return "fail";
            }
            if (order.getStatus() == 1) {
                log.info("订单已处理过，跳过: {}", outTradeNo);
                return "success";
            }
            // 更新订单状态
            userMapper.updateOrderStatus(outTradeNo, tradeNo ,1);

            // 执行扩容操作
            userMapper.expandUserSpace(order.getCreateBy(), extractTargetSpace(order.getOrderName()));

            log.info("支付宝支付处理成功, orderId: {}", outTradeNo);
        }

        // 5. 返回 success 给支付宝（必须！否则支付宝会重复通知）
        return "success";
    }

    @Override
    public void saveUserChat(UserChat userChat) {
        logMapper.aveUserChat(userChat);
    }

    @Override
    public void updateChatLog(String chatId, String summaryContent, String answerStatus) {
        logMapper.updateChatLog(chatId ,summaryContent ,answerStatus);
    }

    @Override
    public ChatLogVo getChat(String tmp, String uid) {
        return logMapper.getChat(tmp, uid);
    }

    private String extractTargetSpace(String orderName) {
        // "用户空间扩容到1000" -> 提取 "1000"
        if (orderName.contains("扩容到")) {
            return orderName.substring(orderName.indexOf("扩容到"));
        }
        return "100";
    }

}
