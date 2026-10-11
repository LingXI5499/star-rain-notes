/*
 * SearchRateLimitService：搜索模块的业务契约文件。
 * 业务范围：搜索文档投影、索引重建和关键词查询。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：SearchRateLimitedException。
 * 阅读入口：check（检查搜索Rate上限）。
 */

package com.starrainnotes.search.service;

import com.starrainnotes.search.exception.SearchRateLimitedException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 搜索Rate上限业务服务，属于搜索模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
@Service
public class SearchRateLimitService {
    /**
     * MINUTE。
     * 初始值为 <code>60</code>。
     */
    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    /**
     * 盐值。
     */
    private final byte[] salt = new byte[32];
    /**
     * 时间窗口。
     */
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    /**
     * 构造SearchRateLimitService，接收本类使用的依赖或初始化参数。
     */
    public SearchRateLimitService() { new SecureRandom().nextBytes(salt); }

    /**
     * 检查搜索Rate上限。
     * 主要协作调用：<code>windows.size</code>、<code>windows.entrySet</code>、<code>windows.compute</code>。
     * 实现中的显式失败类型包括 <code>SearchRateLimitedException</code>；具体触发条件见对应分支。
     *
     * @param remoteAddress 远端地址；类型为 <code>String</code>
     */
    public void check(String remoteAddress) {
        String key = hash(remoteAddress);
        long minute = Instant.now().getEpochSecond() / 60;
        if (windows.size() > 10_000) windows.entrySet().removeIf(entry -> entry.getValue().minute < minute);
        Window window = windows.compute(key, (ignored, old) ->
                old == null || old.minute != minute ? new Window(minute, 1) : new Window(minute, old.count + 1));
        if (window.count > MAX_REQUESTS_PER_MINUTE) throw new SearchRateLimitedException();
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
            digest.update(salt);
            return HexFormat.of().formatHex(digest.digest(address.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    /**
     * 时间窗口，属于搜索模块。
     * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
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
