package com.example.myApp.demos.service.impl;

import com.example.myApp.demos.service.PyScriptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class PyScriptServiceImpl implements PyScriptService {


    @Value("${script.path}")
    private String path;

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    public void callBsScript(String taskId, String url) {
        executor.submit(() -> {
            try {
                //构建py命令
                String scriptPath = path + "/" + "bs.py";
                String[] cmd = new String[]{"python3", scriptPath, taskId, url};

                //启动进程
                ProcessBuilder processBuilder = new ProcessBuilder(cmd);
                Process process = processBuilder.start();

                // 读取脚本输出（必须读取，否则进程可能阻塞）
                InputStream inputStream = process.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("Python脚本输出: " + line);
                }

                //等待进程完成
                int exitCode = process.waitFor();
                log.info("Python 脚本执行完成，退出码: {}", exitCode);

            } catch (Exception e) {
                log.error("调用 Python 脚本失败: {}", e.getMessage());
            }
        });
    }
}
