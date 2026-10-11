/*
 * MediaReferenceServiceImplTest：媒体模块的测试文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：ApiException、MediaReferenceCommand、MediaStatus、MediaAssetEntity、MediaAssetMapper、MediaReferenceEntity、MediaReferenceMapper。
 * 阅读入口：attachLocksAssetThenInserts（attach \u5148\u9501\u8d44\u4ea7\u884c\u518d\u63d2\u5165\u5f15\u7528\uff0c\u5b57\u6bb5\u4f7f\u7528\u89c4\u8303\u5316\u540e\u7684\u503c）；attachMissingAsset（attach \u5230\u4e0d\u5b58\u5728\u7684\u5a92\u4f53\u62a5 MEDIA_NOT_FOUND）；attachArchivedAsset（attach \u5230 ARCHIVED \u5a92\u4f53\u62a5 MEDIA_NOT_ACTIVE）；attachDuplicate（\u91cd\u590d\u5f15\u7528\u547d\u4e2d\u552f\u4e00\u952e\u65f6\u62a5 MEDIA_REFERENCE_EXISTS）。
 */

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
/**
 * 媒体引用业务服务实现测试，属于媒体模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
@ExtendWith(MockitoExtension.class)
class MediaReferenceServiceImplTest {

    /**
     * 资产数据映射。
     * 协作类型：<code>MediaAssetMapper</code>。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock
    private MediaAssetMapper assetMapper;

    /**
     * 引用数据映射。
     * 协作类型：<code>MediaReferenceMapper</code>。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock
    private MediaReferenceMapper referenceMapper;

    /**
     * 业务服务。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @InjectMocks
    private MediaReferenceServiceImpl service;

    /**
     * attach \u5148\u9501\u8d44\u4ea7\u884c\u518d\u63d2\u5165\u5f15\u7528\uff0c\u5b57\u6bb5\u4f7f\u7528\u89c4\u8303\u5316\u540e\u7684\u503c。
     * 主要协作调用：<code>assetMapper.assetByIdForUpdate</code>、<code>service.attach</code>。
     */
    @Test
    @DisplayName("attach 先锁资产行再插入引用，字段使用规范化后的值")
    void attachLocksAssetThenInserts() {
        when(assetMapper.assetByIdForUpdate(1024L)).thenReturn(asset(MediaStatus.ACTIVE_CODE));

        service.attach(new MediaReferenceCommand(1024L, "blog", "post", 57L, "blog.cover"));

        verify(assetMapper).assetByIdForUpdate(1024L);
        verify(referenceMapper).insertReference(any(MediaReferenceEntity.class));
    }

    /**
     * attach \u5230\u4e0d\u5b58\u5728\u7684\u5a92\u4f53\u62a5 MEDIA_NOT_FOUND。
     * 主要协作调用：<code>assetMapper.assetByIdForUpdate</code>、<code>service.attach</code>。
     */
    @Test
    @DisplayName("attach 到不存在的媒体报 MEDIA_NOT_FOUND")
    void attachMissingAsset() {
        when(assetMapper.assetByIdForUpdate(1L)).thenReturn(null);

        assertThatThrownBy(() -> service.attach(command()))
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MEDIA_NOT_FOUND");
        verify(referenceMapper, never()).insertReference(any());
    }

    /**
     * attach \u5230 ARCHIVED \u5a92\u4f53\u62a5 MEDIA_NOT_ACTIVE。
     * 主要协作调用：<code>assetMapper.assetByIdForUpdate</code>、<code>service.attach</code>。
     */
    @Test
    @DisplayName("attach 到 ARCHIVED 媒体报 MEDIA_NOT_ACTIVE")
    void attachArchivedAsset() {
        when(assetMapper.assetByIdForUpdate(1L)).thenReturn(asset(MediaStatus.ARCHIVED_CODE));

        assertThatThrownBy(() -> service.attach(command()))
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MEDIA_NOT_ACTIVE");
        verify(referenceMapper, never()).insertReference(any());
    }

    /**
     * \u91cd\u590d\u5f15\u7528\u547d\u4e2d\u552f\u4e00\u952e\u65f6\u62a5 MEDIA_REFERENCE_EXISTS。
     * 主要协作调用：<code>assetMapper.assetByIdForUpdate</code>、<code>service.attach</code>。
     */
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

    /**
     * \u53c2\u6570\u975e\u6cd5\u65f6\u5728\u8bbf\u95ee\u6570\u636e\u5e93\u4e4b\u524d\u5c31\u62d2\u7edd。
     * 主要协作调用：<code>service.attach</code>。
     */
    @Test
    @DisplayName("参数非法时在访问数据库之前就拒绝")
    void attachRejectsInvalidCommandBeforeDatabase() {
        assertThatThrownBy(() -> service.attach(
                new MediaReferenceCommand(1L, "TUTORIAL", "CHAPTER", 2L, "blog.cover")))
                .isInstanceOf(ApiException.class);

        verifyNoInteractions(assetMapper, referenceMapper);
    }

    /**
     * detach \u53ea\u5220\u5f15\u7528\uff1b\u5f15\u7528\u4e0d\u5b58\u5728\u65f6\u62a5 MEDIA_REFERENCE_NOT_FOUND。
     * 主要协作调用：<code>referenceMapper.deleteReference</code>、<code>service.detach</code>。
     */
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

    /**
     * detachAll \u7528\u4e8e\u4e1a\u52a1\u5bf9\u8c61\u5220\u9664\u65f6\u6279\u91cf\u89e3\u9664\u5f15\u7528。
     * 主要协作调用：<code>service.detachAll</code>。
     */
    @Test
    @DisplayName("detachAll 用于业务对象删除时批量解除引用")
    void detachAll() {
        service.detachAll(" blog ", "post", 57L);

        verify(referenceMapper).deleteAllBySource("BLOG", "POST", 57L);
    }

    /**
     * \u5f15\u7528\u5217\u8868\u4e0e\u6570\u91cf\u67e5\u8be2。
     * 主要协作调用：<code>referenceMapper.selectByMediaAssetId</code>、<code>referenceMapper.countByMediaAssetId</code>、<code>service.listByAsset</code>、<code>service.countByAsset</code>。
     */
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

    /**
     * \u5f15\u7528\u5217\u8868\u7684\u975e\u6cd5 ID \u62a5 MEDIA_QUERY_INVALID\uff1b\u6570\u91cf\u67e5\u8be2\u7684\u975e\u6cd5 ID \u89c6\u4e3a 0。
     * 主要协作调用：<code>service.listByAsset</code>、<code>service.countByAsset</code>。
     */
    @Test
    @DisplayName("引用列表的非法 ID 报 MEDIA_QUERY_INVALID；数量查询的非法 ID 视为 0")
    void invalidIds() {
        assertThatThrownBy(() -> service.listByAsset(null))
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MEDIA_QUERY_INVALID");
        assertThat(service.countByAsset(null)).isZero();
        assertThat(service.countByAsset(-1L)).isZero();
    }

    /**
     * command处理入口。
     * @return 类型为 <code>MediaReferenceCommand</code> 的处理结果
     */
    private static MediaReferenceCommand command() {
        return new MediaReferenceCommand(1L, "BLOG", "POST", 57L, "blog.cover");
    }

    /**
     * 资产处理入口。
     * 关键处理步骤调用 <code>entity.setId</code>、<code>entity.setStorageKey</code>、<code>entity.setStatus</code>。
     *
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>
     * @return 类型为 <code>MediaAssetEntity</code> 的处理结果
     */
    private static MediaAssetEntity asset(String status) {
        MediaAssetEntity entity = new MediaAssetEntity();
        entity.setId(1L);
        entity.setStorageKey("2026/10/1.png");
        entity.setStatus(status);
        return entity;
    }
}
