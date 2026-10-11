package com.starrainnotes.media.service.impl;

import com.starrainnotes.media.properties.MediaProperties;
import com.starrainnotes.media.mapper.MediaAssetMapper;
import com.starrainnotes.media.service.MediaCleanupService;
import com.starrainnotes.media.storage.MediaStorage;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/*
 * 孤儿文件清理实现，由定时任务触发。
 *
 * 判定规则（两条同时满足才删）：
 *   1. storageKey 在 sr_media_asset 中没有记录；
 *   2. 文件写入时间早于宽限期（默认 24 小时）。
 * 第 2 条是关键：刚落盘、还没来得及写库的正常上传文件不能被误删。
 */
@Service
@ConditionalOnProperty(prefix = "star-rain.media.orphan-cleanup", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class MediaCleanupServiceImpl implements MediaCleanupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaCleanupServiceImpl.class);

    private final MediaStorage storage;
    private final MediaAssetMapper assetMapper;
    private final MediaProperties properties;

    public MediaCleanupServiceImpl(MediaStorage storage,
                                   MediaAssetMapper assetMapper,
                                   MediaProperties properties) {
        this.storage = storage;
        this.assetMapper = assetMapper;
        this.properties = properties;
    }

    @Override
    @Scheduled(cron = "${star-rain.media.orphan-cleanup.cron:0 30 3 * * *}")
    public int cleanupOrphans() {
        int graceHours = properties.getOrphanCleanup().getGraceHours();
        Instant threshold = Instant.now().minus(graceHours, ChronoUnit.HOURS);
        Set<String> known = new HashSet<>(assetMapper.allStorageKeys());

        int scanned = 0;
        int removed = 0;
        int failed = 0;
        for (String key : storage.listKeys()) {
            scanned++;
            if (known.contains(key)) {
                continue;
            }
            Instant modified = storage.lastModified(key);
            /*
             * 拿不到写入时间就保守跳过。
             * 清理是破坏性操作：宁可漏删一个孤儿文件，也不能误删正常文件。
             * 宽限期内同样跳过，因为可能是“已落盘、还没写库”的正常上传。
             */
            if (modified == null || modified.isAfter(threshold)) {
                continue;
            }
            try {
                storage.delete(key);
                removed++;
                LOGGER.warn("清理孤儿媒体文件：storageKey={}", key);
            } catch (RuntimeException ex) {
                failed++;
                LOGGER.error("孤儿媒体文件清理失败：storageKey={}", key, ex);
            }
        }
        LOGGER.info("孤儿媒体文件清理完成：扫描={}，已删除={}，失败={}，宽限期={}小时",
                scanned, removed, failed, graceHours);
        return removed;
    }
}
