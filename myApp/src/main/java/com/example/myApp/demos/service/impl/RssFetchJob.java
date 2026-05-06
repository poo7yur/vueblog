//package com.example.myApp.demos.service.impl;
//
//import cn.hutool.core.util.RandomUtil;
//import cn.hutool.core.util.StrUtil;
//import com.example.myApp.demos.entity.Essay;
//import com.example.myApp.demos.entity.UserRssRel;
//import com.example.myApp.demos.mapper.EssayMapper;
//import com.example.myApp.demos.service.PyScriptService;
//import com.rometools.rome.feed.synd.SyndEntry;
//import com.rometools.rome.feed.synd.SyndFeed;
//import com.rometools.rome.io.SyndFeedInput;
//import com.rometools.rome.io.XmlReader;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import java.net.URL;
//import java.text.SimpleDateFormat;
//import java.util.*;
//
//@Service
//@Slf4j
//public class RssFetchJob {
//    @Value("${output.dir}")
//    private String outputDir;
//
//    @Autowired
//    private EssayMapper essayMapper;
//
//    @Autowired
//    private PyScriptService pyScriptService;
//
//    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//
//    //@Scheduled(cron = "0 */10 * * * ?")
//    public void requestWebRssApi() {
//        List<Essay> essays = new ArrayList<>();
//        Date now = new Date();
//        List<UserRssRel> userRssRels = essayMapper.getUserRssJob();
//        userRssRels.forEach(userRssRel -> {
//            try {
//                URL url = new URL(userRssRel.getRssUrl());
//                SyndFeed feed = new SyndFeedInput().build(new XmlReader(url.openStream()));
//                for (SyndEntry entry : feed.getEntries()) {
//                    String taskId = RandomUtil.randomNumbers(10);
//                    Essay essay = new Essay();
//                    essay.setId(taskId);
//                    essay.setTitle(entry.getTitle());
//                    String articleUrl = entry.getLink();
//                    pyScriptService.invokeBsScript(taskId, articleUrl, r -> handleSuccess(taskId, r, 0), e -> handleFailure(taskId, e));
//                    essay.setUpdateTime(now);
//                    essay.setType(1);
//                    essay.setCreateUser(userRssRel.getUserId());
//                    essay.setStoragePath(outputDir + "/" + taskId + ".html");
//                    essay.setIsPublic(0);
//                    essays.add(essay);
//                }
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        });
//        if (!essays.isEmpty()) {
//            essayMapper.batchSaveEssay(essays);
//        }
//        log.info("{}---处理了{}篇文章", sdf.format(now), essays.size());
//    }
//
//    private void handleSuccess(String taskId, String result, int isPublic) {
//        log.info("{}处理成功", taskId);
//        if (StrUtil.isNotEmpty(result)) essayMapper.updateEssay(new Essay(taskId, result, result, isPublic));
//    }
//
//    private void handleFailure(String taskId, Throwable error) {
//        log.error("{}处理失败：{}", taskId, error.getMessage());
//        essayMapper.delEssayById(taskId);
//    }
//}
