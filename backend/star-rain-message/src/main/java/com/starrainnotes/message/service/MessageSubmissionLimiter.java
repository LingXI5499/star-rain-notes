/*
 * MessageSubmissionLimiter：留言模块的业务契约文件。
 * 业务范围：留言提交、审核展示和后台管理。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：ApiException。
 * 阅读入口：check（检查留言SubmissionLimiter）。
 */

package com.starrainnotes.message.service;

import com.starrainnotes.common.exception.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 留言SubmissionLimiter，属于留言模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
@Component
public class MessageSubmissionLimiter {
    /**
     * MINUTE。
     * 初始值为 <code>5</code>。
     */
    private static final int MAX_PER_MINUTE = 5;
    /**
     * MILLIS。
     * 初始值为 <code>60000</code>。
     */
    private static final long WINDOW_MILLIS = 60_000;
    /**
     * secret。
     */
    private final byte[] secret = new byte[32];
    /**
     * 时间窗口。
     */
    private final Map<String, Window> windows = new HashMap<>();

    /**
     * 构造MessageSubmissionLimiter，接收本类使用的依赖或初始化参数。
     */
    public MessageSubmissionLimiter() {
        new SecureRandom().nextBytes(secret);
    }

    /**
     * 检查留言SubmissionLimiter。
     * 主要协作调用：<code>windows.size</code>、<code>windows.entrySet</code>、<code>windows.clear</code>、<code>windows.get</code>、<code>windows.put</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     *
     * @param remoteAddress 远端地址；类型为 <code>String</code>
     */
    public synchronized void check(String remoteAddress) {
        long now = System.currentTimeMillis();
        if (windows.size() > 2048) {
            windows.entrySet().removeIf(entry -> now - entry.getValue().startedAt >= WINDOW_MILLIS);
        }
        if (windows.size() > 4096) {
            windows.clear();
        }
        String key = fingerprint(remoteAddress == null ? "unknown" : remoteAddress);
        Window window = windows.get(key);
        if (window == null || now - window.startedAt >= WINDOW_MILLIS) {
            windows.put(key, new Window(now, 1));
            return;
        }
        if (window.count >= MAX_PER_MINUTE) {
            throw new ApiException("MESSAGE_RATE_LIMITED", "留言过于频繁，请稍后再试", 429);
        }
        window.count++;
    }

    /**
     * fingerprint处理入口。
     * 关键处理步骤调用 <code>MessageDigest.getInstance</code>、<code>digest.update</code>、<code>Base64.getEncoder</code>、<code>digest.digest</code>。
     * 实现中的显式失败类型包括 <code>IllegalStateException</code>；具体触发条件见对应分支。
     *
     * @param address 地址；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private String fingerprint(String address) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(secret);
            return Base64.getEncoder().encodeToString(digest.digest(address.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    /**
     * 时间窗口，属于留言模块。
     * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
     */
    private static class Window {
        /**
         * started时间。
         */
        private final long startedAt;
        /**
         * 数量。
         */
        private int count;

        /**
         * 构造Window，接收本类使用的依赖或初始化参数。
         *
         * @param startedAt started时间；类型为 <code>long</code>
         * @param count 数量；类型为 <code>int</code>
         */
        private Window(long startedAt, int count) {
            this.startedAt = startedAt;
            this.count = count;
        }
    }
}
