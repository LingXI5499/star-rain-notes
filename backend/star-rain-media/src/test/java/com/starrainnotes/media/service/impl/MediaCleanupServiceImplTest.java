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
@ExtendWith(MockitoExtension.class)
class MediaCleanupServiceImplTest {

    @Mock
    private MediaStorage storage;

    @Mock
    private MediaAssetMapper assetMapper;

    private MediaCleanupServiceImpl service;

    @BeforeEach
    void setUp() {
        MediaProperties properties = new MediaProperties();
        properties.getOrphanCleanup().setGraceHours(24);
        service = new MediaCleanupServiceImpl(storage, assetMapper, properties);
    }

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

    @Test
    @DisplayName("拿不到写入时间的孤儿文件保守跳过，不冒险删除")
    void skipsWhenLastModifiedUnknown() {
        when(assetMapper.allStorageKeys()).thenReturn(List.of());
        when(storage.listKeys()).thenReturn(List.of("2026/10/unknown.png"));
        when(storage.lastModified("2026/10/unknown.png")).thenReturn(null);

        assertThat(service.cleanupOrphans()).isZero();
        verify(storage, never()).delete("2026/10/unknown.png");
    }

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

    @Test
    @DisplayName("没有孤儿文件时什么都不做")
    void nothingToDo() {
        when(assetMapper.allStorageKeys()).thenReturn(List.of("2026/10/known.png"));
        when(storage.listKeys()).thenReturn(List.of("2026/10/known.png"));

        assertThat(service.cleanupOrphans()).isZero();
        verify(storage, never()).delete(org.mockito.ArgumentMatchers.anyString());
    }
}
