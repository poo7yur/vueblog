package com.example.myApp.demos.service.impl;

import com.example.myApp.demos.service.PyScriptService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Service
@Slf4j
public class PyScriptServiceImpl implements PyScriptService {

    @Value("${script.path}")
    private String path;

    // 存储任务 Future，用于追踪和取消
    private final ConcurrentHashMap<String, Future<?>> taskFutures = new ConcurrentHashMap<>();

    // 使用有界队列和自定义线程工厂，避免资源耗尽
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            2,                                      // 核心线程数
            10,                                     // 最大线程数
            60L,                                    // 空闲线程存活时间
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),         // 有界队列，防止 OOM
            new ThreadFactory() {
                private final AtomicInteger count = new AtomicInteger(0);

                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "python-exec-" + count.incrementAndGet());
                    t.setDaemon(true);  // 设为守护线程，避免阻塞应用关闭
                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()  // 拒绝策略：由调用线程执行
    );

    @PreDestroy
    public void shutdown() {
        log.info("正在关闭 Python 脚本执行线程池...");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    public void invokeBsScript(String taskId, String url, Consumer<String> onSuccess, Consumer<Throwable> onError) {
        String[] cmd;
        String scriptPath;
        if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
            //windows中noti环境python地址
            String pythonPath = "C:\\Users\\Admin\\.conda\\envs\\noti\\python.exe";
            scriptPath = path + "/bs.py";
            cmd = new String[]{pythonPath, scriptPath, taskId, url};
        } else {
            //bs.py放在的位置
            scriptPath = path + "/" + "bs.py";
            cmd = new String[]{"python3", scriptPath, taskId, url};
        }
        // 提交任务并保存 Future
        Future<?> future = executor.submit(() -> {
            Process process = null;
            StringBuilder output = new StringBuilder();
            StringBuilder errorOutput = new StringBuilder();

            try {
                log.info("开始执行 Python 任务 [{}]", taskId);

                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.redirectErrorStream(false);

                process = pb.start();

                // 异步读取标准输出（避免阻塞）
                Future<String> outputFuture = readStreamAsync(process.getInputStream(), output);
                Future<String> errorFuture = readStreamAsync(process.getErrorStream(), errorOutput);

                // 设置超时（5分钟）
                boolean finished = process.waitFor(5, TimeUnit.MINUTES);
                if (!finished) {
                    process.destroyForcibly();
                    throw new TimeoutException("Python 脚本执行超时（5分钟）");
                }

                // 等待流读取完成
                outputFuture.get(5, TimeUnit.SECONDS);
                errorFuture.get(5, TimeUnit.SECONDS);

                int exitCode = process.exitValue();

                if (exitCode != 0) {
                    throw new RuntimeException(
                            String.format("Python 脚本执行失败，退出码: %d, 错误: %s",
                                    exitCode, errorOutput)
                    );
                }

                // 检查输出是否为空
                String result = output.toString().trim();
                if (StringUtils.isEmpty(result)) {
                    throw new RuntimeException("Python 脚本返回空结果");
                }

                log.info("任务 [{}] 执行成功，输出: {}", taskId, result);

                // 成功回调
                if (onSuccess != null) {
                    onSuccess.accept(result);
                }

            } catch (Exception e) {
                log.error("任务 [{}] 执行失败: {}", taskId, e.getMessage(), e);

                // 失败回调
                if (onError != null) {
                    onError.accept(e);
                }

                // 可选：发送告警通知（钉钉/邮件等）
                sendAlert(taskId, url, e.getMessage());

            } finally {
                taskFutures.remove(taskId);
                if (process != null && process.isAlive()) {
                    process.destroyForcibly();
                }
            }
        });

        taskFutures.put(taskId, future);

    }

    /**
     * 异步读取流，避免阻塞
     */
    private Future<String> readStreamAsync(InputStream is, StringBuilder sb) {
        return executor.submit(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                    log.debug("Python 输出: {}", line);
                }
                return sb.toString();
            }
        });
    }

    /**
     * 取消指定任务
     */
    public boolean cancelTask(String taskId) {
        Future<?> future = taskFutures.get(taskId);
        if (future != null) {
            boolean cancelled = future.cancel(true);
            log.info("任务 [{}] 取消状态: {}", taskId, cancelled);
            return cancelled;
        }
        return false;
    }

    /**
     * 发送失败告警（示例）
     */
    private void sendAlert(String taskId, String url, String errorMsg) {
        // 集成钉钉/企业微信/邮件通知
        log.warn("发送告警通知 - 任务ID: {}, URL: {}, 错误: {}", taskId, url, errorMsg);
    }
}
