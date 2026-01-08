package com.example.myApp.demos.aop;

import com.alibaba.fastjson.JSONObject;
import com.example.myApp.demos.Constants;
import com.example.myApp.demos.entity.LogEntity;
import com.example.myApp.demos.mapper.LogMapper;
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
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AssessLogAspect {

    private final RocketMQTemplate rocketMQTemplate;

    @Resource
    private LogMapper logMapper;

    @Pointcut("@annotation(accessLog)")
    public void pointcut(AccessLog accessLog) {
    }

    @Around("pointcut(accessLog)")
    public Object around(ProceedingJoinPoint joinPoint, AccessLog accessLog) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        LogEntity logEntity = new LogEntity();
        logEntity.setModule(accessLog.module());
        logEntity.setDescription(accessLog.description());
        logEntity.setIp(getIp(request));
        logEntity.setLoginUser("root");
        logEntity.setUri(request.getRequestURI());
        logEntity.setMethod(request.getMethod());
        logEntity.setParams(getParamMapStr(joinPoint));
        logEntity.setAccessTime(System.currentTimeMillis());

        try {
            Object result = joinPoint.proceed();
            String resultJson = JSONObject.toJSONString(result);
            logEntity.setResult(resultJson);
            logMapper.saveAccessLog(logEntity);
            return result;
        } catch (Exception e) {
            logEntity.setResult(e.getMessage());
            throw e;
        } finally {
            logEntity.setResult(JSONObject.toJSONString(logEntity.getResult()));
            rocketMQTemplate.asyncSend(Constants.ACCESS_LOG_TOPIC, logEntity, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    System.out.println("AccessLog sent ok");
                }

                @Override
                public void onException(Throwable e) {
                    System.out.println("AccessLog sent fail");
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

    private String getParamMapStr(ProceedingJoinPoint joinPoint) {
        Map<String, Object> map = new HashMap<>();
        // 1. 获取 URL 参数 (GET参数)
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        Map<String, String[]> urlParams = request.getParameterMap();
        // 将参数放入map，如果是数组则转为字符串
        urlParams.forEach((k, v) -> map.put(k, v.length == 1 ? v : Arrays.toString(v)));
        // 2. 获取方法参数 (POST Body 或 方法入参)
        Object[] args = joinPoint.getArgs();

        if (request.getMethod().equals("POST")) {
            for (Object arg : args) {
                // 跳过 null 值
                if (arg == null) {
                    continue;
                }

                // 跳过 ServletRequest 和 ServletResponse
                if (arg instanceof HttpServletRequest || arg instanceof HttpServletResponse) {
                    continue;
                }

                // 核心改造：判断是否为 MultipartFile 或包含 MultipartFile 的对象
                if (arg instanceof MultipartFile) {
                    // 如果是单个文件，只记录文件名和大小，不记录文件内容
                    MultipartFile file = (MultipartFile) arg;
                    map.put("file", String.format("FileName: %s, Size: %d bytes", file.getOriginalFilename(), file.getSize()));
                } else if (arg instanceof Collection) {
                    // 如果是集合（可能包含文件列表）
                    List<Object> processedList = new ArrayList<>();
                    for (Object item : (Collection<?>) arg) {
                        if (item instanceof MultipartFile) {
                            MultipartFile file = (MultipartFile) item;
                            processedList.add(String.format("FileItem: %s", file.getOriginalFilename()));
                        } else {
                            // 非文件对象正常处理
                            processedList.add(item);
                        }
                    }
                    // 这里可以使用索引作为key，或者根据业务逻辑处理
                    map.put("collectionParam", processedList);
                } else {
                    // 普通对象或复杂对象处理
                    for (Object ag : args) {
                        if (!(ag instanceof HttpServletRequest) && !(ag instanceof HttpServletResponse)) {
                            map.putAll(JSONObject.parseObject(JSONObject.toJSONString(ag), Map.class));
                        }
                    }
                }
            }
        }
        return JSONObject.toJSONString(map);
    }
}
