package com.example.myApp.demos.controller;

import com.example.myApp.demos.Constants;
import com.example.myApp.demos.dto.EssayDto;
import com.example.myApp.demos.dto.LinkDto;
import com.example.myApp.demos.dto.PageDto;
import com.example.myApp.demos.entity.Essay;
import com.example.myApp.demos.entity.R;
import com.example.myApp.demos.service.EssayService;
import com.example.myApp.demos.service.FileOptService;
import com.example.myApp.demos.util.JwtUtil;
import com.github.pagehelper.PageInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

@RestController
public class EssayController {

    @Resource
    private EssayService essayService;

    @Resource
    private FileOptService fileOptService;

    @PostMapping("/createEssay")
    public R<String> createEssay(@RequestBody EssayDto essayDto, HttpServletRequest request) {
        try {
            if (Objects.isNull(essayDto) || StringUtils.isEmpty(essayDto.getTitle()))
                throw new RuntimeException(Constants.ILLEGAl_OPT);
            return R.ok(essayService.createEssay(essayDto, request));
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/queryEssay")
    public R<PageInfo<Essay>> queryEssay(@RequestBody PageDto dto, HttpServletRequest request) {
        dto.setUserId(JwtUtil.parseUidFromToken(request));
        return R.ok(essayService.queryEssay(dto));
    }

    @PostMapping("/saveEssayContent")
    public R<String> saveEssayContent(@RequestBody EssayDto essayDto) {
        try {
            if (Objects.isNull(essayDto) || StringUtils.isEmpty(essayDto.getId()))
                throw new RuntimeException(Constants.ILLEGAl_OPT);
            return R.ok(essayService.saveEssayContent(essayDto));
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/publishEssay")
    public R<String> publishEssay(@RequestBody EssayDto essayDto) {
        try {
            if (Objects.isNull(essayDto) || 1 != essayDto.getStatus())
                throw new RuntimeException(Constants.ILLEGAl_OPT);
            return R.ok(essayService.publishEssay(essayDto));
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/deleteEssay")
    public R<String> deleteEssay(@RequestBody EssayDto essayDto, HttpServletRequest request) {
        try {
            if (Objects.isNull(essayDto) || StringUtils.isEmpty(essayDto.getId()))
                throw new RuntimeException(Constants.ILLEGAl_OPT);
            return R.ok(essayService.deleteEssay(essayDto));
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/getEssayContent")
    public void getEssayContent(@RequestBody EssayDto essayDto, HttpServletResponse response) {
        try {
            // 检查参数是否有效
            if (Objects.isNull(essayDto) || StringUtils.isEmpty(essayDto.getStoragePath())) {
                throw new RuntimeException(Constants.FILE_NOT_FOUND);
            }
            String fileName = essayDto.getStoragePath().substring(essayDto.getStoragePath().lastIndexOf("/") + 1);
            writeByStream(essayDto, response, fileName);
        } catch (Exception e) {
            // 设置错误响应状态
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try {
                response.getWriter().write("获取文件内容失败: " + e.getMessage());
            } catch (IOException ioException) {
                throw new RuntimeException("无法写入错误响应: " + ioException.getMessage());
            }
        }
    }

    @PostMapping("/saveLink")
    public R<String> saveLink(@RequestBody LinkDto dto, HttpServletRequest request) {
        try {
            dto.setUser(JwtUtil.parseUidFromToken(request));
            String msg = essayService.saveLink(dto);
            return R.ok(msg);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    private void writeByStream(EssayDto essayDto, HttpServletResponse response, String fileName) throws Exception {
        // 获取文件输入流
        String storagePath = essayDto.getStoragePath();

        // 获取操作系统类型（Windows: "Windows XP/7/10/Server"，Linux: "Linux"，Mac: "Mac OS X"）
        String osName = System.getProperty("os.name").toLowerCase();
        if(osName.contains("windows")){
            if (storagePath.startsWith("/")) {
                storagePath = storagePath.substring(1); // 去掉开头的/
            }
            // 替换Linux路径分隔符为Windows分隔符，拼接D:
            storagePath = "D:\\" + storagePath.replace("/", "\\");
        }

        InputStream ins = fileOptService.readFullPathFile(storagePath);

        // 设置响应头
        response.setContentType("application/octet-stream"); // 通用二进制流类型
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

        // 将输入流数据写入响应输出流
        try (java.io.OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = ins.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
        } catch (IOException e) {
            throw new RuntimeException("文件传输失败: " + e.getMessage());
        } finally {
            ins.close(); // 确保输入流关闭
        }
    }
}


