/*
 * MediaAssetServiceImplTest：媒体模块的测试文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：ApiException、PageResult、MediaAccessLevel、MediaStatus、MediaQueryDTO、MediaAssetEntity、MediaAssetMapper。
 * 阅读入口：pageRejectsInvalidPageSize（\u67e5\u8be2\u53c2\u6570\u8d8a\u754c\u8fd4\u56de MEDIA_QUERY_INVALID \u800c\u4e0d\u662f\u9759\u9ed8\u7ea0\u6b63）；pageRejectsInvalidFilter（\u7b5b\u9009\u503c\u975e\u6cd5\u65f6\u660e\u786e\u62a5\u9519\uff0c\u4e0d\u9759\u9ed8\u5ffd\u7565）；pageNormalizesFilters（\u7b5b\u9009\u503c\u5927\u5c0f\u5199\u4e0d\u654f\u611f\uff0c\u7a7a\u767d\u89c6\u4e3a\u4e0d\u7b5b\u9009）；detailCarriesReferenceCount（\u8be6\u60c5\u8fd4\u56de\u5f15\u7528\u6570\u91cf\uff0c\u4f9b\u5220\u9664\u524d\u5224\u65ad）。
 */

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
/**
 * 媒体资产业务服务实现测试，属于媒体模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
@ExtendWith(MockitoExtension.class)
class MediaAssetServiceImplTest {

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
    private MediaAssetServiceImpl service;

    /**
     * \u67e5\u8be2\u53c2\u6570\u8d8a\u754c\u8fd4\u56de MEDIA_QUERY_INVALID \u800c\u4e0d\u662f\u9759\u9ed8\u7ea0\u6b63。
     * 主要协作调用：<code>service.page</code>。
     */
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

    /**
     * \u7b5b\u9009\u503c\u975e\u6cd5\u65f6\u660e\u786e\u62a5\u9519\uff0c\u4e0d\u9759\u9ed8\u5ffd\u7565。
     * 主要协作调用：<code>service.page</code>。
     */
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

    /**
     * \u7b5b\u9009\u503c\u5927\u5c0f\u5199\u4e0d\u654f\u611f\uff0c\u7a7a\u767d\u89c6\u4e3a\u4e0d\u7b5b\u9009。
     * 主要协作调用：<code>assetMapper.assetPageCount</code>、<code>assetMapper.assetPage</code>、<code>service.page</code>。
     */
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

    /**
     * \u8be6\u60c5\u8fd4\u56de\u5f15\u7528\u6570\u91cf\uff0c\u4f9b\u5220\u9664\u524d\u5224\u65ad。
     * 主要协作调用：<code>assetMapper.assetById</code>、<code>referenceMapper.countByMediaAssetId</code>、<code>service.getDetail</code>。
     */
    @Test
    @DisplayName("详情返回引用数量，供删除前判断")
    void detailCarriesReferenceCount() {
        when(assetMapper.assetById(7L)).thenReturn(asset(7L, MediaStatus.ACTIVE_CODE));
        when(referenceMapper.countByMediaAssetId(7L)).thenReturn(3L);

        assertThat(service.getDetail(7L).getReferenceCount()).isEqualTo(3L);
    }

    /**
     * \u5a92\u4f53\u4e0d\u5b58\u5728\u65f6\u62a5 MEDIA_NOT_FOUND。
     * 主要协作调用：<code>assetMapper.assetById</code>、<code>service.getDetail</code>。
     */
    @Test
    @DisplayName("媒体不存在时报 MEDIA_NOT_FOUND")
    void detailNotFound() {
        when(assetMapper.assetById(404L)).thenReturn(null);

        assertThatThrownBy(() -> service.getDetail(404L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MEDIA_NOT_FOUND");
    }

    /**
     * \u5b58\u5728\u4e1a\u52a1\u5f15\u7528\u65f6\u7981\u6b62\u5f52\u6863\uff1a\u5f15\u7528\u5b8c\u6574\u6027\u4f18\u5148\u4e8e\u7ba1\u7406\u4fbf\u5229\uff0c\u6ca1\u6709 force \u53c2\u6570。
     * 主要协作调用：<code>assetMapper.assetByIdForUpdate</code>、<code>referenceMapper.countByMediaAssetId</code>、<code>service.archive</code>。
     */
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

    /**
     * \u5df2\u5f52\u6863\u5a92\u4f53\u4e0d\u80fd\u91cd\u590d\u5f52\u6863。
     * 主要协作调用：<code>assetMapper.assetByIdForUpdate</code>、<code>service.archive</code>。
     */
    @Test
    @DisplayName("已归档媒体不能重复归档")
    void archiveRejectedWhenAlreadyArchived() {
        when(assetMapper.assetByIdForUpdate(7L)).thenReturn(asset(7L, MediaStatus.ARCHIVED_CODE));

        assertThatThrownBy(() -> service.archive(7L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MEDIA_NOT_ACTIVE");
    }

    /**
     * \u5f52\u6863\u4e0d\u5b58\u5728\u7684\u5a92\u4f53\u62a5 MEDIA_NOT_FOUND。
     * 主要协作调用：<code>assetMapper.assetByIdForUpdate</code>、<code>service.archive</code>。
     */
    @Test
    @DisplayName("归档不存在的媒体报 MEDIA_NOT_FOUND")
    void archiveRejectedWhenMissing() {
        when(assetMapper.assetByIdForUpdate(404L)).thenReturn(null);

        assertThatThrownBy(() -> service.archive(404L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MEDIA_NOT_FOUND");
    }

    /**
     * \u65e0\u5f15\u7528\u65f6\u5f52\u6863\u6210\u529f。
     * 主要协作调用：<code>assetMapper.assetByIdForUpdate</code>、<code>referenceMapper.countByMediaAssetId</code>、<code>assetMapper.archiveAsset</code>、<code>service.archive</code>。
     */
    @Test
    @DisplayName("无引用时归档成功")
    void archiveSucceedsWhenUnreferenced() {
        when(assetMapper.assetByIdForUpdate(7L)).thenReturn(asset(7L, MediaStatus.ACTIVE_CODE));
        when(referenceMapper.countByMediaAssetId(7L)).thenReturn(0L);
        when(assetMapper.archiveAsset(eq(7L), any(LocalDateTime.class))).thenReturn(1);

        service.archive(7L);

        verify(assetMapper).archiveAsset(eq(7L), any(LocalDateTime.class));
    }

    /**
     * \u6761\u4ef6\u66f4\u65b0\u5f71\u54cd 0 \u884c\u65f6\u5b81\u53ef\u62a5\u9519\uff0c\u4e5f\u4e0d\u9759\u9ed8\u5f53\u4f5c\u6210\u529f。
     * 主要协作调用：<code>assetMapper.assetByIdForUpdate</code>、<code>referenceMapper.countByMediaAssetId</code>、<code>assetMapper.archiveAsset</code>、<code>service.archive</code>。
     */
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

    /**
     * \u53ea\u6709 ARCHIVED \u624d\u80fd\u6062\u590d。
     * 主要协作调用：<code>assetMapper.assetByIdForUpdate</code>、<code>service.restore</code>。
     */
    @Test
    @DisplayName("只有 ARCHIVED 才能恢复")
    void restoreOnlyFromArchived() {
        when(assetMapper.assetByIdForUpdate(7L)).thenReturn(asset(7L, MediaStatus.ACTIVE_CODE));

        assertThatThrownBy(() -> service.restore(7L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MEDIA_NOT_ACTIVE");
    }

    /**
     * \u6062\u590d\u5f52\u6863\u5a92\u4f53\u6210\u529f。
     * 主要协作调用：<code>assetMapper.assetByIdForUpdate</code>、<code>assetMapper.restoreAsset</code>、<code>service.restore</code>。
     */
    @Test
    @DisplayName("恢复归档媒体成功")
    void restoreSucceeds() {
        when(assetMapper.assetByIdForUpdate(7L)).thenReturn(asset(7L, MediaStatus.ARCHIVED_CODE));
        when(assetMapper.restoreAsset(7L)).thenReturn(1);

        service.restore(7L);

        verify(assetMapper).restoreAsset(7L);
    }

    /**
     * \u8c03\u6574\u8bbf\u95ee\u7ea7\u522b\u5199\u5165\u89c4\u8303\u5316\u679a\u4e3e\u540d。
     * 主要协作调用：<code>assetMapper.assetById</code>、<code>service.changeAccessLevel</code>。
     */
    @Test
    @DisplayName("调整访问级别写入规范化枚举名")
    void changeAccessLevel() {
        when(assetMapper.assetById(7L)).thenReturn(asset(7L, MediaStatus.ACTIVE_CODE));

        service.changeAccessLevel(7L, MediaAccessLevel.PUBLIC);

        verify(assetMapper).updateAccessLevel(7L, "PUBLIC");
    }

    /**
     * assertUsable\uff1a\u4e0d\u5b58\u5728\uff08\u542b null\uff09\u62a5 NOT_FOUND\uff0c\u5df2\u5f52\u6863\u62a5 NOT_ACTIVE\uff0cACTIVE \u901a\u8fc7。
     * 主要协作调用：<code>assetMapper.assetById</code>、<code>service.assertUsable</code>、<code>service.isActive</code>、<code>service.get</code>、<code>service.exists</code>。
     */
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

    /**
     * \u6a21\u5757\u95f4\u6458\u8981\u4e0d\u542b storageKey \u7b49\u5185\u90e8\u5b57\u6bb5。
     * 主要协作调用：<code>assetMapper.assetById</code>、<code>service.get</code>。
     */
    @Test
    @DisplayName("模块间摘要不含 storageKey 等内部字段")
    void summaryHidesStorageDetails() {
        when(assetMapper.assetById(8L)).thenReturn(asset(8L, MediaStatus.ACTIVE_CODE));

        var summary = service.get(8L);

        assertThat(summary.getContentUrl()).isEqualTo("/api/media/assets/8/content");
        assertThat(summary.toString()).doesNotContain("storageKey").doesNotContain("2026/10/");
    }

    /**
     * 资产处理入口。
     * 关键处理步骤调用 <code>entity.setId</code>、<code>entity.setOriginalName</code>、<code>entity.setMediaType</code>、<code>entity.setMimeType</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>
     * @return 类型为 <code>MediaAssetEntity</code> 的处理结果
     */
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
