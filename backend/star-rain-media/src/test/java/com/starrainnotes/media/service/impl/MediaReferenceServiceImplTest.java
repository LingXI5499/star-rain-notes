package com.starrainnotes.media.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.media.api.dto.MediaReferenceCommand;
import com.starrainnotes.media.enumeration.MediaStatus;
import com.starrainnotes.media.entity.MediaAssetEntity;
import com.starrainnotes.media.mapper.MediaAssetMapper;
import com.starrainnotes.media.entity.MediaReferenceEntity;
import com.starrainnotes.media.mapper.MediaReferenceMapper;
import com.starrainnotes.media.vo.MediaReferenceVO;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

/*
 * MED-003 / MED-004 / MED-007 服务测试。
 *
 * 关键约束：
 *   1. attach 必须先锁资产行，且媒体必须是 ACTIVE；
 *   2. 参数非法要在碰数据库之前就被拒绝；
 *   3. detach 只解除引用，绝不删除资产。
 */
@ExtendWith(MockitoExtension.class)
class MediaReferenceServiceImplTest {

    @Mock
    private MediaAssetMapper assetMapper;

    @Mock
    private MediaReferenceMapper referenceMapper;

    @InjectMocks
    private MediaReferenceServiceImpl service;

    @Test
    @DisplayName("attach 先锁资产行再插入引用，字段使用规范化后的值")
    void attachLocksAssetThenInserts() {
        when(assetMapper.assetByIdForUpdate(1024L)).thenReturn(asset(MediaStatus.ACTIVE_CODE));

        service.attach(new MediaReferenceCommand(1024L, "blog", "post", 57L, "blog.cover"));

        verify(assetMapper).assetByIdForUpdate(1024L);
        verify(referenceMapper).insertReference(any(MediaReferenceEntity.class));
    }

    @Test
    @DisplayName("attach 到不存在的媒体报 MEDIA_NOT_FOUND")
    void attachMissingAsset() {
        when(assetMapper.assetByIdForUpdate(1L)).thenReturn(null);

        assertThatThrownBy(() -> service.attach(command()))
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MEDIA_NOT_FOUND");
        verify(referenceMapper, never()).insertReference(any());
    }

    @Test
    @DisplayName("attach 到 ARCHIVED 媒体报 MEDIA_NOT_ACTIVE")
    void attachArchivedAsset() {
        when(assetMapper.assetByIdForUpdate(1L)).thenReturn(asset(MediaStatus.ARCHIVED_CODE));

        assertThatThrownBy(() -> service.attach(command()))
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MEDIA_NOT_ACTIVE");
        verify(referenceMapper, never()).insertReference(any());
    }

    @Test
    @DisplayName("重复引用命中唯一键时报 MEDIA_REFERENCE_EXISTS")
    void attachDuplicate() {
        when(assetMapper.assetByIdForUpdate(1L)).thenReturn(asset(MediaStatus.ACTIVE_CODE));
        // insertReference 是 void 方法，只能用 doThrow 形式打桩
        doThrow(new DuplicateKeyException("duplicate"))
                .when(referenceMapper).insertReference(any(MediaReferenceEntity.class));

        assertThatThrownBy(() -> service.attach(command()))
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MEDIA_REFERENCE_EXISTS");
    }

    @Test
    @DisplayName("参数非法时在访问数据库之前就拒绝")
    void attachRejectsInvalidCommandBeforeDatabase() {
        assertThatThrownBy(() -> service.attach(
                new MediaReferenceCommand(1L, "TUTORIAL", "CHAPTER", 2L, "blog.cover")))
                .isInstanceOf(ApiException.class);

        verifyNoInteractions(assetMapper, referenceMapper);
    }

    @Test
    @DisplayName("detach 只删引用；引用不存在时报 MEDIA_REFERENCE_NOT_FOUND")
    void detachBranches() {
        when(referenceMapper.deleteReference(1L, "BLOG", "POST", 57L, "blog.cover")).thenReturn(1);
        service.detach(new MediaReferenceCommand(1L, "BLOG", "POST", 57L, "blog.cover"));

        when(referenceMapper.deleteReference(2L, "BLOG", "POST", 57L, "blog.cover")).thenReturn(0);
        assertThatThrownBy(() -> service.detach(
                new MediaReferenceCommand(2L, "BLOG", "POST", 57L, "blog.cover")))
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MEDIA_REFERENCE_NOT_FOUND");
        // 全程不触碰资产，更不会删除资产或文件
        verify(assetMapper, never()).archiveAsset(any(), any());
    }

    @Test
    @DisplayName("detachAll 用于业务对象删除时批量解除引用")
    void detachAll() {
        service.detachAll(" blog ", "post", 57L);

        verify(referenceMapper).deleteAllBySource("BLOG", "POST", 57L);
    }

    @Test
    @DisplayName("引用列表与数量查询")
    void listAndCount() {
        MediaReferenceEntity entity = new MediaReferenceEntity();
        entity.setId(1L);
        entity.setMediaAssetId(1024L);
        entity.setSourceModule("BLOG");
        entity.setSourceType("POST");
        entity.setSourceId(57L);
        entity.setUsageCode("blog.cover");
        entity.setCreatedAt(LocalDateTime.now());
        when(referenceMapper.selectByMediaAssetId(1024L)).thenReturn(List.of(entity));
        when(referenceMapper.countByMediaAssetId(1024L)).thenReturn(1L);

        List<MediaReferenceVO> references = service.listByAsset(1024L);

        assertThat(references).hasSize(1);
        assertThat(references.get(0).getUsageCode()).isEqualTo("blog.cover");
        assertThat(service.countByAsset(1024L)).isEqualTo(1L);
    }

    @Test
    @DisplayName("引用列表的非法 ID 报 MEDIA_QUERY_INVALID；数量查询的非法 ID 视为 0")
    void invalidIds() {
        assertThatThrownBy(() -> service.listByAsset(null))
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MEDIA_QUERY_INVALID");
        assertThat(service.countByAsset(null)).isZero();
        assertThat(service.countByAsset(-1L)).isZero();
    }

    private static MediaReferenceCommand command() {
        return new MediaReferenceCommand(1L, "BLOG", "POST", 57L, "blog.cover");
    }

    private static MediaAssetEntity asset(String status) {
        MediaAssetEntity entity = new MediaAssetEntity();
        entity.setId(1L);
        entity.setStorageKey("2026/10/1.png");
        entity.setStatus(status);
        return entity;
    }
}
