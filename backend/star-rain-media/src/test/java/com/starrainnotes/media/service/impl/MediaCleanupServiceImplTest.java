/*
 * MediaCleanupServiceImplTest：媒体模块的测试文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：ApiException、MediaProperties、MediaAssetMapper、MediaStorage。
 * 阅读入口：setUp（初始化当前测试场景的被测对象与模拟依赖）；deletesOnlyAgedOrphans（\u53ea\u5220\u300c\u6570\u636e\u5e93\u65e0\u8bb0\u5f55\u300d\u4e14\u300c\u8d85\u8fc7\u5bbd\u9650\u671f\u300d\u7684\u6587\u4ef6）；skipsWhenLastModifiedUnknown（\u62ff\u4e0d\u5230\u5199\u5165\u65f6\u95f4\u7684\u5b64\u513f\u6587\u4ef6\u4fdd\u5b88\u8df3\u8fc7\uff0c\u4e0d\u5192\u9669\u5220\u9664）；continuesAfterSingleFailure（\u5355\u4e2a\u6587\u4ef6\u5220\u9664\u5931\u8d25\u4e0d\u5f71\u54cd\u5176\u5b83\u6587\u4ef6\uff0c\u4e5f\u4e0d\u4e2d\u65ad\u4efb\u52a1）。
 */

package com.starrainnotes.media.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.media.properties.MediaProperties;
import com.starrainnotes.media.mapper.MediaAssetMapper;
import com.starrainnotes.media.storage.MediaStorage;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/*
 * 孤儿文件清理测试。
 *
 * 这是文件系统与数据库不一致时的兜底手段，误删代价高，
 * 所以两条判定条件（无数据库记录 + 超过宽限期）必须都验证到。
 */
/**
 * 媒体清理业务服务实现测试，属于媒体模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
@ExtendWith(MockitoExtension.class)
class MediaCleanupServiceImplTest {

    /**
     * 存储。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock
    private MediaStorage storage;

    /**
     * 资产数据映射。
     * 协作类型：<code>MediaAssetMapper</code>。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock
    private MediaAssetMapper assetMapper;

    /**
     * 业务服务。
     */
    private MediaCleanupServiceImpl service;

    /**
     * 初始化当前测试场景的被测对象与模拟依赖。
     * 关键处理步骤调用 <code>properties.getOrphanCleanup</code>。
     */
    @BeforeEach
    void setUp() {
        MediaProperties properties = new MediaProperties();
        properties.getOrphanCleanup().setGraceHours(24);
        service = new MediaCleanupServiceImpl(storage, assetMapper, properties);
    }

    /**
     * \u53ea\u5220\u300c\u6570\u636e\u5e93\u65e0\u8bb0\u5f55\u300d\u4e14\u300c\u8d85\u8fc7\u5bbd\u9650\u671f\u300d\u7684\u6587\u4ef6。
     * 主要协作调用：<code>assetMapper.allStorageKeys</code>、<code>storage.listKeys</code>、<code>storage.lastModified</code>、<code>service.cleanupOrphans</code>。
     */
    @Test
    @DisplayName("只删「数据库无记录」且「超过宽限期」的文件")
    void deletesOnlyAgedOrphans() {
        when(assetMapper.allStorageKeys()).thenReturn(List.of("2026/10/known.png"));
        when(storage.listKeys()).thenReturn(List.of(
                "2026/10/known.png", "2026/10/fresh-orphan.png", "2026/10/old-orphan.png"));
        // 宽限期内：可能正处于“已落盘、未写库”的正常上传过程中
        when(storage.lastModified("2026/10/fresh-orphan.png")).thenReturn(Instant.now());
        when(storage.lastModified("2026/10/old-orphan.png"))
                .thenReturn(Instant.now().minus(48, ChronoUnit.HOURS));

        int removed = service.cleanupOrphans();

        assertThat(removed).isEqualTo(1);
        verify(storage).delete("2026/10/old-orphan.png");
        verify(storage, never()).delete("2026/10/known.png");
        verify(storage, never()).delete("2026/10/fresh-orphan.png");
    }

    /**
     * \u62ff\u4e0d\u5230\u5199\u5165\u65f6\u95f4\u7684\u5b64\u513f\u6587\u4ef6\u4fdd\u5b88\u8df3\u8fc7\uff0c\u4e0d\u5192\u9669\u5220\u9664。
     * 主要协作调用：<code>assetMapper.allStorageKeys</code>、<code>storage.listKeys</code>、<code>storage.lastModified</code>、<code>service.cleanupOrphans</code>。
     */
    @Test
    @DisplayName("拿不到写入时间的孤儿文件保守跳过，不冒险删除")
    void skipsWhenLastModifiedUnknown() {
        when(assetMapper.allStorageKeys()).thenReturn(List.of());
        when(storage.listKeys()).thenReturn(List.of("2026/10/unknown.png"));
        when(storage.lastModified("2026/10/unknown.png")).thenReturn(null);

        assertThat(service.cleanupOrphans()).isZero();
        verify(storage, never()).delete("2026/10/unknown.png");
    }

    /**
     * \u5355\u4e2a\u6587\u4ef6\u5220\u9664\u5931\u8d25\u4e0d\u5f71\u54cd\u5176\u5b83\u6587\u4ef6\uff0c\u4e5f\u4e0d\u4e2d\u65ad\u4efb\u52a1。
     * 主要协作调用：<code>assetMapper.allStorageKeys</code>、<code>storage.listKeys</code>、<code>storage.lastModified</code>、<code>service.cleanupOrphans</code>。
     */
    @Test
    @DisplayName("单个文件删除失败不影响其它文件，也不中断任务")
    void continuesAfterSingleFailure() {
        when(assetMapper.allStorageKeys()).thenReturn(List.of());
        when(storage.listKeys()).thenReturn(List.of("2026/10/a.png", "2026/10/b.png"));
        Instant old = Instant.now().minus(48, ChronoUnit.HOURS);
        when(storage.lastModified("2026/10/a.png")).thenReturn(old);
        when(storage.lastModified("2026/10/b.png")).thenReturn(old);
        org.mockito.Mockito.doThrow(new ApiException("MEDIA_STORAGE_DELETE_FAILED", "锁定", 500))
                .when(storage).delete("2026/10/a.png");

        int removed = service.cleanupOrphans();

        assertThat(removed).isEqualTo(1);
        verify(storage).delete("2026/10/b.png");
    }

    /**
     * \u6ca1\u6709\u5b64\u513f\u6587\u4ef6\u65f6\u4ec0\u4e48\u90fd\u4e0d\u505a。
     * 主要协作调用：<code>assetMapper.allStorageKeys</code>、<code>storage.listKeys</code>、<code>service.cleanupOrphans</code>。
     */
    @Test
    @DisplayName("没有孤儿文件时什么都不做")
    void nothingToDo() {
        when(assetMapper.allStorageKeys()).thenReturn(List.of("2026/10/known.png"));
        when(storage.listKeys()).thenReturn(List.of("2026/10/known.png"));

        assertThat(service.cleanupOrphans()).isZero();
        verify(storage, never()).delete(org.mockito.ArgumentMatchers.anyString());
    }
}
