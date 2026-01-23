package com.example.myApp.demos.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.example.myApp.demos.Constants;
import com.example.myApp.demos.dto.EssayDto;
import com.example.myApp.demos.dto.LinkDto;
import com.example.myApp.demos.dto.PageDto;
import com.example.myApp.demos.entity.Essay;
import com.example.myApp.demos.mapper.EssayMapper;
import com.example.myApp.demos.service.EssayService;
import com.example.myApp.demos.service.FileOptService;
import com.example.myApp.demos.service.PyScriptService;
import com.example.myApp.demos.util.JwtUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EssayServiceImpl implements EssayService {

    @Resource
    private EssayMapper essayMapper;

    @Resource
    private FileOptService fileOptService;

    @Resource
    private PyScriptService pyScriptService;

    @Value("${output.dir}")
    private String outputDir;

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

    private static final String ESSAY = "/essay/";

    @Override
    public PageInfo<Essay> queryEssay(PageDto dto) {
        PageHelper.startPage(dto.getPageNum(), dto.getPageSize());
        Integer share = 1;
        if (!StringUtils.isEmpty(dto.getUserId())) share = null;
        List<Essay> list = essayMapper.queryEssay(dto, share);
        return new PageInfo<>(list);
    }

    @Override
    public String publishEssay(EssayDto essayDto) {
        essayMapper.updateShare(essayDto.getId(), essayDto.getStatus());
        return "发布成功";
    }

    @Override
    public String deleteEssay(EssayDto essayDto) {
        essayMapper.delEssayById(essayDto.getId());
        return "删除成功";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveEssayContent(EssayDto essayDto) throws Exception {
        String htmlContent = essayDto.getContent();
        String storagePath = essayDto.getStoragePath();
        String title = essayDto.getTitle();
        String id = essayDto.getId();
        String summary = getSummary(htmlContent);
        essayMapper.updateEssay(new Essay(id, title, summary, new Date()));
        fileOptService.uploadHtmlFile(storagePath, htmlContent);
        return "保存成功";
    }

    private String getSummary(String htmlContent) {
        if (StringUtils.isBlank(htmlContent)) {
            return "";
        }

        String pureText = htmlContent;

        // 移除所有HTML标签
        Pattern htmlTagPattern = Pattern.compile("<[^>]+>", Pattern.CASE_INSENSITIVE);
        Matcher htmlTagMatcher = htmlTagPattern.matcher(pureText);
        pureText = htmlTagMatcher.replaceAll("");

        // 移除HTML转义字符
        pureText = pureText.replaceAll("&nbsp;", " ")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&amp;", "&")
                .replaceAll("&quot;", "\"")
                .replaceAll("&apos;", "'");

        // 清理多余空格/换行
        pureText = pureText.replaceAll("\\s+", " ").trim();

        // 截取前200字符
        int maxLength = 200;
        return pureText.length() <= maxLength ? pureText : pureText.substring(0, maxLength);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createEssay(EssayDto essayDto, HttpServletRequest request) throws Exception {
        String userId = JwtUtil.parseUidFromToken(request);
        String userName = JwtUtil.parseUserFromToken(request);
        if (StringUtils.isEmpty(userId) || StringUtils.isEmpty(userName))
            throw new RuntimeException(Constants.TOKEN_EXPIRED);
        String id = RandomUtil.randomNumbers(6);
        Date date = new Date();
        String ymd = sdf.format(date);
        Essay essay = new Essay();
        essay.setId(id);
        essay.setTitle(essayDto.getTitle());
        essay.setUpdateTime(date);
        essay.setType(0);
        essay.setCreateUser(userId);
        String fullpath = ESSAY + userId + "/" + ymd + "/" + id + ".txt";
        essay.setStoragePath(fullpath);
        essayMapper.createEssay(essay);
        fileOptService.uploadHtmlFile(fullpath, "");
        return "创建成功";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveLink(LinkDto dto) {
        String taskId = RandomUtil.randomNumbers(10);
        String user = dto.getUser();
        if (StringUtils.isEmpty(user)) throw new RuntimeException(Constants.TOKEN_EXPIRED);
        String url = dto.getLinkUrl();
        if (StringUtils.isEmpty(url)) throw new RuntimeException(Constants.ERROR_LINK_URL);
        //调用py脚本 生成一个html
        pyScriptService.callBsScript(taskId, url);
        //保存到essay表
        Essay essay = new Essay();
        essay.setId(taskId);
        essay.setTitle(url);
        essay.setUpdateTime(new Date());
        essay.setType(1);
        essay.setCreateUser(user);
        essay.setStoragePath(outputDir + taskId + ".html");
        essayMapper.createEssay(essay);
        return "保存成功";
    }
}
