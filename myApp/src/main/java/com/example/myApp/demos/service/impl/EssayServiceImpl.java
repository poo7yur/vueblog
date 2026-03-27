package com.example.myApp.demos.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.example.myApp.demos.Constants;
import com.example.myApp.demos.dto.EssayDto;
import com.example.myApp.demos.dto.LinkDto;
import com.example.myApp.demos.dto.PageDto;
import com.example.myApp.demos.entity.Essay;
import com.example.myApp.demos.entity.UserRssRel;
import com.example.myApp.demos.mapper.EssayMapper;
import com.example.myApp.demos.service.EssayService;
import com.example.myApp.demos.service.FileOptService;
import com.example.myApp.demos.service.PyScriptService;
import com.example.myApp.demos.util.IpUtil;
import com.example.myApp.demos.util.JwtUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class EssayServiceImpl implements EssayService {

    @Resource
    private EssayMapper essayMapper;

    @Resource
    private FileOptService fileOptService;

    @Resource
    private PyScriptService pyScriptService;

    @Value("${output.dir}")
    private String outputDir;
    @Value("${server.port:8080}")
    private Integer serverPort;

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

    private static final String ESSAY = "/essay/";

    @Override
    public PageInfo<Essay> queryEssay(PageDto dto) {
        PageHelper.startPage(dto.getPageNum(), dto.getPageSize());
        String uid = dto.getUserId();
        List<Essay> list = essayMapper.queryEssay(dto, uid);
        if (dto.getType() == 2) list.forEach(essay -> essay.setStoragePath(pathToUrl(essay.getStoragePath())));
        return new PageInfo<>(list);
    }

    private String pathToUrl(String storagePath) {
        StringBuilder sb = new StringBuilder();
        sb.append("http://").append(IpUtil.getLocalIp()).append(":").append(serverPort);
        // 截取出从book开始的字符串且 windows路径转/
        String normalizedPath = storagePath.replace("\\", "/").replace("epub", "jpg");
        int bookIndex = normalizedPath.indexOf("/book/");
        // 截取出从 book 开始的路径
        String relativePath = normalizedPath.substring(bookIndex);
        //返回完整的路径
        String encodedPath = relativePath.replaceAll(" ", "%20"); // 空格转%20
        sb.append("/static").append(encodedPath);
        return sb.toString();
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
        essayMapper.updateEssay(new Essay(id, title, summary, essayDto.getIsPublic()));
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
        String userId = JwtUtil.parseUid(request);
        String userName = JwtUtil.parseUser(request);
        if (StringUtils.isEmpty(userId) || StringUtils.isEmpty(userName))
            throw new RuntimeException(Constants.TOKEN_EXPIRED);
        String id = RandomUtil.randomNumbers(8);
        Date date = new Date();
        String ymd = sdf.format(date);
        Essay essay = new Essay();
        essay.setId(id);
        essay.setTitle(essayDto.getTitle());
        essay.setUpdateTime(date);
        essay.setType(0);
        essay.setCreateUser(userId);
        String fullPath = ESSAY + userId + "/" + ymd + "/" + id + ".txt";
        essay.setStoragePath(fullPath);
        essayMapper.createEssay(essay);
        fileOptService.uploadHtmlFile(fullPath, "");
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
        int isPublic = dto.getIsPublic();
        Essay essay = new Essay();
        essay.setId(taskId);
        essay.setTitle("");
        essay.setUpdateTime(new Date());
        essay.setType(1);
        essay.setCreateUser(user);
        essay.setStoragePath(outputDir + "/" + taskId + ".html");
        essayMapper.createEssay(essay);
        //调用py脚本 生成一个html
        pyScriptService.invokeBsScript(taskId, url, r -> handleSuccess(taskId, r, isPublic), e -> handleFailure(taskId, e));
        return "保存成功";
    }

    @Override
    public String addRss(UserRssRel dto) {
        if(StringUtils.isEmpty(dto.getRssUrl()) ||StringUtils.isEmpty(dto.getCorn()))
            throw new RuntimeException(Constants.PARAM_ERR);
        essayMapper.addRss(dto);
        return String.valueOf(dto.getId());
    }

    // 成功处理
    private void handleSuccess(String taskId, String result, int isPublic) {
        log.info("{}处理成功", taskId);
        //把result更新到文章标题
        if (StrUtil.isNotEmpty(result)) essayMapper.updateEssay(new Essay(taskId, result, result, isPublic));
    }

    // 失败处理
    private void handleFailure(String taskId, Throwable error) {
        log.error("{}处理失败：{}", taskId, error.getMessage());
        essayMapper.delEssayById(taskId);//删除记录保持原子性
    }
}
