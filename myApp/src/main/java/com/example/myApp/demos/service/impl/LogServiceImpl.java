package com.example.myApp.demos.service.impl;

import com.example.myApp.demos.Constants;
import com.example.myApp.demos.dto.PageDto;
import com.example.myApp.demos.entity.MsgEntity;
import com.example.myApp.demos.mapper.LogMapper;
import com.example.myApp.demos.service.LogService;
import com.example.myApp.demos.util.DirUtil;
import com.example.myApp.demos.util.JwtUtil;
import com.example.myApp.demos.vo.SongVo;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import io.jsonwebtoken.Claims;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.*;

@Service
public class LogServiceImpl implements LogService {

    @Value("${music.store}")
    private String musicStore;

    @Resource
    private LogMapper logMapper;

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

}
