package com.starrainnotes.media.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.media.enumeration.MediaAccessLevel;
import com.starrainnotes.media.enumeration.MediaStatus;
import com.starrainnotes.media.dto.MediaQueryDTO;
import com.starrainnotes.media.entity.MediaAssetEntity;
import com.starrainnotes.media.mapper.MediaAssetMapper;
import com.starrainnotes.media.vo.MediaAssetVO;
import com.starrainnotes.media.mapper.MediaReferenceMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/*
 * 媒体资产服务单元测试。
 * 重点是归档的引用完整性保护与条件更新结果判断，以及查询参数的错误码语义。
 */
@ExtendWith(MockitoExtension.class)
class MediaAssetServiceImplTest {

    @Mock
    private MediaAssetMapper assetMapper;

    @Mock
    private MediaReferenceMapper referenceMapper;

    @InjectMocks
    private MediaAssetServiceImpl service;

    @Test
    @DisplayName("查询参数越界返回 MEDIA_QUERY_INVALID 而不是静默纠正")
    void pageRejectsInvalidPageSize() {
        MediaQueryDTO query = new MediaQueryDTO();
        query.setPageSize(500);

        assertThatThrownBy(() -> service.page(query))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MEDIA_QUERY_INVALID");
    }

    @Test
    @DisplayName("筛选值非法时明确报错，不静默忽略")
    void pageRejectsInvalidFilter() {
        MediaQueryDTO query = new MediaQueryDTO();
        query.setMediaType("GIF");

        assertThatThrownBy(() -> service.page(query))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MEDIA_QUERY_INVALID");
    }

    @Test
    @DisplayName("筛选值大小写不敏感，空白视为不筛选")
    void pageNormalizesFilters() {
        MediaQueryDTO query = new MediaQueryDTO();
        query.setMediaType("image");
        query.setStatus("  active ");
        query.setAccessLevel("public");
        when(assetMapper.assetPageCount(isNull(), eq("IMAGE"), eq("ACTIVE"), eq("PUBLIC"))).thenReturn(1L);
        when(assetMapper.assetPage(isNull(), eq("IMAGE"), eq("ACTIVE"), eq("PUBLIC"), eq(0), eq(20)))
                .thenReturn(List.of(asset(7L, MediaStatus.ACTIVE_CODE)));

        PageResult<MediaAssetVO> result = service.page(query);

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("详情返回引用数量，供删除前判断")
    void detailCarriesReferenceCount() {
        when(assetMapper.assetById(7L)).thenReturn(asset(7L, MediaStatus.ACTIVE_CODE));
        when(referenceMapper.countByMediaAssetId(7L)).thenReturn(3L);

        assertThat(service.getDetail(7L).getReferenceCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("媒体不存在时报 MEDIA_NOT_FOUND")
    void detailNotFound() {
        when(assetMapper.assetById(404L)).thenReturn(null);

        assertThatThrownBy(() -> service.getDetail(404L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MEDIA_NOT_FOUND");
    }

    @Test
    @DisplayName("存在业务引用时禁止归档：引用完整性优先于管理便利，没有 force 参数")
    void archiveRejectedWhenInUse() {
        when(assetMapper.assetByIdForUpdate(7L)).thenReturn(asset(7L, MediaStatus.ACTIVE_CODE));
        when(referenceMapper.countByMediaAssetId(7L)).thenReturn(2L);

        assertThatThrownBy(() -> service.archive(7L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MEDIA_ASSET_IN_USE");
    }

    @Test
    @DisplayName("已归档媒体不能重复归档")
    void archiveRejectedWhenAlreadyArchived() {
        when(assetMapper.assetByIdForUpdate(7L)).thenReturn(asset(7L, MediaStatus.ARCHIVED_CODE));

        assertThatThrownBy(() -> service.archive(7L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MEDIA_NOT_ACTIVE");
    }

    @Test
    @DisplayName("归档不存在的媒体报 MEDIA_NOT_FOUND")
    void archiveRejectedWhenMissing() {
        when(assetMapper.assetByIdForUpdate(404L)).thenReturn(null);

        assertThatThrownBy(() -> service.archive(404L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MEDIA_NOT_FOUND");
    }

    @Test
    @DisplayName("无引用时归档成功")
    void archiveSucceedsWhenUnreferenced() {
        when(assetMapper.assetByIdForUpdate(7L)).thenReturn(asset(7L, MediaStatus.ACTIVE_CODE));
        when(referenceMapper.countByMediaAssetId(7L)).thenReturn(0L);
        when(assetMapper.archiveAsset(eq(7L), any(LocalDateTime.class))).thenReturn(1);

        service.archive(7L);

        verify(assetMapper).archiveAsset(eq(7L), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("条件更新影响 0 行时宁可报错，也不静默当作成功")
    void archiveReportsConflictWhenConditionalUpdateMisses() {
        when(assetMapper.assetByIdForUpdate(7L)).thenReturn(asset(7L, MediaStatus.ACTIVE_CODE));
        when(referenceMapper.countByMediaAssetId(7L)).thenReturn(0L);
        when(assetMapper.archiveAsset(eq(7L), any(LocalDateTime.class))).thenReturn(0);

        assertThatThrownBy(() -> service.archive(7L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MEDIA_NOT_ACTIVE");
    }

    @Test
    @DisplayName("只有 ARCHIVED 才能恢复")
    void restoreOnlyFromArchived() {
        when(assetMapper.assetByIdForUpdate(7L)).thenReturn(asset(7L, MediaStatus.ACTIVE_CODE));

        assertThatThrownBy(() -> service.restore(7L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MEDIA_NOT_ACTIVE");
    }

    @Test
    @DisplayName("恢复归档媒体成功")
    void restoreSucceeds() {
        when(assetMapper.assetByIdForUpdate(7L)).thenReturn(asset(7L, MediaStatus.ARCHIVED_CODE));
        when(assetMapper.restoreAsset(7L)).thenReturn(1);

        service.restore(7L);

        verify(assetMapper).restoreAsset(7L);
    }

    @Test
    @DisplayName("调整访问级别写入规范化枚举名")
    void changeAccessLevel() {
        when(assetMapper.assetById(7L)).thenReturn(asset(7L, MediaStatus.ACTIVE_CODE));

        service.changeAccessLevel(7L, MediaAccessLevel.PUBLIC);

        verify(assetMapper).updateAccessLevel(7L, "PUBLIC");
    }

    @Test
    @DisplayName("assertUsable：不存在（含 null）报 NOT_FOUND，已归档报 NOT_ACTIVE，ACTIVE 通过")
    void assertUsableBranches() {
        when(assetMapper.assetById(404L)).thenReturn(null);
        when(assetMapper.assetById(7L)).thenReturn(asset(7L, MediaStatus.ARCHIVED_CODE));
        when(assetMapper.assetById(8L)).thenReturn(asset(8L, MediaStatus.ACTIVE_CODE));

        assertThatThrownBy(() -> service.assertUsable(404L))
                .extracting(ex -> ((ApiException) ex).getCode()).isEqualTo("MEDIA_NOT_FOUND");
        assertThatThrownBy(() -> service.assertUsable(null))
                .extracting(ex -> ((ApiException) ex).getCode()).isEqualTo("MEDIA_NOT_FOUND");
        assertThatThrownBy(() -> service.assertUsable(7L))
                .extracting(ex -> ((ApiException) ex).getCode()).isEqualTo("MEDIA_NOT_ACTIVE");

        service.assertUsable(8L);
        assertThat(service.isActive(8L)).isTrue();
        assertThat(service.get(404L)).isNull();
        assertThat(service.exists(null)).isFalse();
    }

    @Test
    @DisplayName("模块间摘要不含 storageKey 等内部字段")
    void summaryHidesStorageDetails() {
        when(assetMapper.assetById(8L)).thenReturn(asset(8L, MediaStatus.ACTIVE_CODE));

        var summary = service.get(8L);

        assertThat(summary.getContentUrl()).isEqualTo("/api/media/assets/8/content");
        assertThat(summary.toString()).doesNotContain("storageKey").doesNotContain("2026/10/");
    }

    private static MediaAssetEntity asset(Long id, String status) {
        MediaAssetEntity entity = new MediaAssetEntity();
        entity.setId(id);
        entity.setOriginalName("cover.png");
        entity.setMediaType("IMAGE");
        entity.setMimeType("image/png");
        entity.setFileExtension("png");
        entity.setSizeBytes(1024L);
        entity.setSha256("a".repeat(64));
        entity.setStorageProvider("LOCAL");
        entity.setStorageKey("2026/10/" + id + ".png");
        entity.setAccessLevel("PROTECTED");
        entity.setStatus(status);
        entity.setUploadedByAccountId(1L);
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }
}
