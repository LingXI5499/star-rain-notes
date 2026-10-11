/*
 * MediaUploadLimiter：媒体模块的安全策略文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 处理认证、授权或访问限制相关规则；身份来源、限制条件和失败分支以具体方法为准。
 * 主要协作类型：MediaUploadRateLimitedException。
 * 阅读入口：check（检查媒体上传Limiter）。
 */

package com.starrainnotes.media.security;

import com.starrainnotes.media.exception.MediaUploadRateLimitedException;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

/* 单实例按账号限制上传次数，避免连续文件写入耗尽存储和处理资源。 */
/**
 * 媒体上传Limiter，属于媒体模块。
 * 处理认证、授权或访问限制相关规则；身份来源、限制条件和失败分支以具体方法为准。
 */
@Component
public class MediaUploadLimiter {

    /**
     * MINUTE。
     * 初始值为 <code>20</code>。
     */
    private static final int MAX_UPLOADS_PER_MINUTE = 20;
    /**
     * MILLIS。
     */
    private static final long WINDOW_MILLIS = Duration.ofMinutes(1).toMillis();
    /**
     * 时间窗口。
     */
    private final ConcurrentHashMap<Long, Window> windows = new ConcurrentHashMap<>();
    /**
     * clock。
     */
    private final Clock clock;

    /**
     * 构造MediaUploadLimiter，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>this</code>、<code>Clock.systemUTC</code>。
     */
    public MediaUploadLimiter() {
        this(Clock.systemUTC());
    }

    /**
     * 构造MediaUploadLimiter，接收本类使用的依赖或初始化参数。
     *
     * @param clock clock；类型为 <code>Clock</code>
     */
    MediaUploadLimiter(Clock clock) {
        this.clock = clock;
    }

    /**
     * 检查媒体上传Limiter。
     * 主要协作调用：<code>clock.millis</code>、<code>windows.size</code>、<code>windows.entrySet</code>、<code>windows.compute</code>。
     * 实现中的显式失败类型包括 <code>IllegalArgumentException</code>、<code>MediaUploadRateLimitedException</code>；具体触发条件见对应分支。
     * 边界判断包含 <code>(accountId == null || accountId &lt;= 0)</code>；<code>(windows.size() &gt; 4096)</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>
     */
    public void check(Long accountId) {
        if (accountId == null || accountId <= 0) {
            throw new IllegalArgumentException("Authenticated account ID is required");
        }
        long now = clock.millis();
        if (windows.size() > 4096) {
            windows.entrySet().removeIf(entry -> now - entry.getValue().startedAt >= WINDOW_MILLIS);
        }
        Window window = windows.compute(accountId, (ignored, old) ->
                old == null || now - old.startedAt >= WINDOW_MILLIS
                        ? new Window(now, 1) : new Window(old.startedAt, old.count + 1));
        if (window.count > MAX_UPLOADS_PER_MINUTE) {
            throw new MediaUploadRateLimitedException();
        }
    }

    /**
     * 时间窗口，属于媒体模块。
     * 处理认证、授权或访问限制相关规则；身份来源、限制条件和失败分支以具体方法为准。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Window {
        /**
         * started时间。
         */
        private long startedAt;
        /**
         * 数量。
         */
        private int count;
    }
}
