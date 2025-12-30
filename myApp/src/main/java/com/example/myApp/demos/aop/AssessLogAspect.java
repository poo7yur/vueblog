package com.example.myApp.demos.aop;

import com.alibaba.fastjson.JSONObject;
import com.example.myApp.demos.entity.LogEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AssessLogAspect {

    private final RocketMQTemplate rocketMQTemplate;
    private final static String TOPIC = "access-log-topic";

    @Pointcut("@annotation(accessLog)")
    public void pointcut(AccessLog accessLog) {
    }

    @Around("pointcut(accessLog)")
    public Object around(ProceedingJoinPoint joinPoint, AccessLog accessLog) throws Throwable {
        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        LogEntity logEntity = new LogEntity();
        logEntity.setModule(accessLog.module());
        logEntity.setDescription(accessLog.description());
        logEntity.setIp(getIp(request));
        logEntity.setLoginUser("root");
        logEntity.setUri(request.getRequestURI());
        logEntity.setMethod(request.getMethod());
        logEntity.setParams(getParamMap(joinPoint));
        logEntity.setAccessTime(System.currentTimeMillis());

        try {
            Object result = joinPoint.proceed();
            logEntity.setResult(result);
            return result;
        } catch (Exception e) {
            logEntity.setResult(e.getMessage());
            throw e;
        } finally {
            logEntity.setResult(JSONObject.toJSONString(logEntity.getResult()));
            rocketMQTemplate.asyncSend(TOPIC, logEntity, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    log.debug("AccessLog sent ok");
                }

                @Override
                public void onException(Throwable e) {
                    log.error("AccessLog sent fail", e);
                }
            });
        }
    }

    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private Map<String, Object> getParamMap(ProceedingJoinPoint joinPoint) {
        Map<String, Object> map = new HashMap<>();
        // URL 参数
        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        Map<String, String[]> urlParams = request.getParameterMap();
        urlParams.forEach((k, v) -> map.put(k, v.length == 1 ? v[0] : Arrays.toString(v)));
        // Body 参数（JSON）
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (!(arg instanceof HttpServletRequest || arg instanceof HttpServletResponse)) {
                map.putAll(JSONObject.parseObject(JSONObject.toJSONString(arg), Map.class));
            }
        }
        return map;
    }
}
