/*
 * AnalyticsSubmissionLimiter：访问统计模块的安全策略文件。
 * 业务范围：前台访问事件、按日汇总和统计查询。
 * 处理认证、授权或访问限制相关规则；身份来源、限制条件和失败分支以具体方法为准。
 * 主要协作类型：AnalyticsEventRejectedException。
 * 阅读入口：check（检查统计SubmissionLimiter）。
 */

package com.starrainnotes.analytics.security;

import com.starrainnotes.analytics.exception.AnalyticsEventRejectedException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 统计SubmissionLimiter，属于访问统计模块。
 * 处理认证、授权或访问限制相关规则；身份来源、限制条件和失败分支以具体方法为准。
 */
@Component
public class AnalyticsSubmissionLimiter {
    /**
     * 盐值。
     */
    private static final byte[] SALT = new byte[32];
    /**
     * 时间窗口。
     */
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    static {
        new SecureRandom().nextBytes(SALT);
    }

    /**
     * 检查统计SubmissionLimiter。
     * 主要协作调用：<code>windows.size</code>、<code>windows.entrySet</code>、<code>windows.compute</code>。
     * 实现中的显式失败类型包括 <code>AnalyticsEventRejectedException</code>；具体触发条件见对应分支。
     * 边界判断包含 <code>(windows.size() &gt; 10000)</code>；<code>(old == null || old.minute != minute)</code>。
     *
     * @param address 地址；类型为 <code>String</code>
     */
    public void check(String address) {
        String key = hash(address);
        long minute = Instant.now().getEpochSecond() / 60;
        if (windows.size() > 10_000) {
            windows.entrySet().removeIf(entry -> entry.getValue().minute < minute);
        }
        Window window = windows.compute(key, (ignored, old) -> {
            if (old == null || old.minute != minute) return new Window(minute, 1);
            return new Window(minute, old.count + 1);
        });
        if (window.count > 60) throw new AnalyticsEventRejectedException();
    }

    /**
     * 摘要处理入口。
     * 关键处理步骤调用 <code>MessageDigest.getInstance</code>、<code>digest.update</code>、<code>HexFormat.of</code>、<code>digest.digest</code>。
     * 实现中的显式失败类型包括 <code>IllegalStateException</code>；具体触发条件见对应分支。
     *
     * @param address 地址；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private String hash(String address) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(SALT);
            return HexFormat.of().formatHex(digest.digest(address.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    /**
     * 时间窗口，属于访问统计模块。
     * 处理认证、授权或访问限制相关规则；身份来源、限制条件和失败分支以具体方法为准。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Window {
        /**
         * minute。
         */
        private long minute;
        /**
         * 数量。
         */
        private int count;
    }
}
