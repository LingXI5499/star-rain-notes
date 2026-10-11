/*
 * MediaAccessServiceImplTest：媒体模块的测试文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：MediaType、CurrentActorApi、ApiException、MediaAssetEntity、MediaStatus、MediaAssetMapper、MediaPermissions。
 * 阅读入口：publicActiveReadableByAnonymous（ACTIVE + PUBLIC\uff1a\u533f\u540d\u53ef\u8bfb\u5b8c\u6574\u5185\u5bb9\uff0c\u4e14\u4e0d\u4f1a\u53bb\u67e5\u5f53\u524d\u4e3b\u4f53）；protectedActiveDeniedForAnonymous（ACTIVE + PROTECTED\uff1a\u533f\u540d\u88ab\u62d2\u7edd\uff0c\u4e14\u4e0d\u4f1a\u6253\u5f00\u6587\u4ef6）；protectedActiveReadableWithPermission（ACTIVE + PROTECTED\uff1a\u5177\u5907 media:read \u7684\u7ba1\u7406\u5458\u53ef\u8bfb）；archivedAccess（ARCHIVED\uff1a\u533f\u540d\u62ff\u4e0d\u5230\uff1b\u7ba1\u7406\u5458\u53ef\u9884\u89c8）。
 */

package com.starrainnotes.media.service.impl;

import com.starrainnotes.media.enumeration.MediaType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.media.entity.MediaAssetEntity;
import com.starrainnotes.media.enumeration.MediaStatus;
import com.starrainnotes.media.mapper.MediaAssetMapper;
import com.starrainnotes.media.constant.MediaPermissions;
import com.starrainnotes.media.storage.MediaStorage;
import com.starrainnotes.media.vo.MediaContentVO;
import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/*
 * MED-005 读取授权与 Range 支持测试。
 *
 * 授权矩阵是安全边界：PUBLIC 允许匿名，PROTECTED 与 ARCHIVED 都必须
 * 经过认证且具备 media:read，且权限只能来自认证上下文。
 *
 * Range 是功能边界：音频/视频拖动进度依赖它算对区间。
 */
/**
 * 媒体访问业务服务实现测试，属于媒体模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
@ExtendWith(MockitoExtension.class)
class MediaAccessServiceImplTest {

    /**
     * 键。
     */
    private static final String KEY = "2026/10/1.mp4";

    /**
     * 资产数据映射。
     * 协作类型：<code>MediaAssetMapper</code>。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock
    private MediaAssetMapper assetMapper;

    /**
     * 存储。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock
    private MediaStorage storage;

    /**
     * 提供当前认证操作者身份的跨模块接口。
     * 协作类型：<code>CurrentActorApi</code>。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock
    private CurrentActorApi currentActorApi;

    /**
     * 业务服务。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @InjectMocks
    private MediaAccessServiceImpl service;

    /**
     * ACTIVE + PUBLIC\uff1a\u533f\u540d\u53ef\u8bfb\u5b8c\u6574\u5185\u5bb9\uff0c\u4e14\u4e0d\u4f1a\u53bb\u67e5\u5f53\u524d\u4e3b\u4f53。
     * 主要协作调用：<code>assetMapper.assetById</code>、<code>storage.size</code>、<code>storage.open</code>、<code>service.open</code>。
     */
    @Test
    @DisplayName("ACTIVE + PUBLIC：匿名可读完整内容，且不会去查当前主体")
    void publicActiveReadableByAnonymous() {
        when(assetMapper.assetById(1L)).thenReturn(asset("PUBLIC", MediaStatus.ACTIVE_CODE, "VIDEO", KEY));
        when(storage.size(KEY)).thenReturn(1000L);
        when(storage.open(KEY)).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));

        MediaContentVO content = service.open(1L, null);

        assertThat(content.partial()).isFalse();
        assertThat(content.contentLength()).isEqualTo(1000L);
        assertThat(content.getAccessLevel()).isEqualTo("PUBLIC");
        verify(currentActorApi, never()).currentOptional();
    }

    /**
     * ACTIVE + PROTECTED\uff1a\u533f\u540d\u88ab\u62d2\u7edd\uff0c\u4e14\u4e0d\u4f1a\u6253\u5f00\u6587\u4ef6。
     * 主要协作调用：<code>assetMapper.assetById</code>、<code>currentActorApi.currentOptional</code>、<code>service.open</code>。
     */
    @Test
    @DisplayName("ACTIVE + PROTECTED：匿名被拒绝，且不会打开文件")
    void protectedActiveDeniedForAnonymous() {
        when(assetMapper.assetById(1L)).thenReturn(asset("PROTECTED", MediaStatus.ACTIVE_CODE, "IMAGE", KEY));
        when(currentActorApi.currentOptional()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.open(1L, null))
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MEDIA_ACCESS_DENIED");
        verify(storage, never()).open(anyString());
    }

    /**
     * ACTIVE + PROTECTED\uff1a\u5177\u5907 media:read \u7684\u7ba1\u7406\u5458\u53ef\u8bfb。
     * 主要协作调用：<code>assetMapper.assetById</code>、<code>currentActorApi.currentOptional</code>、<code>storage.size</code>、<code>storage.open</code>、<code>service.open</code>。
     */
    @Test
    @DisplayName("ACTIVE + PROTECTED：具备 media:read 的管理员可读")
    void protectedActiveReadableWithPermission() {
        when(assetMapper.assetById(1L)).thenReturn(asset("PROTECTED", MediaStatus.ACTIVE_CODE, "IMAGE", KEY));
        when(currentActorApi.currentOptional()).thenReturn(Optional.of(actorWith(MediaPermissions.READ)));
        when(storage.size(KEY)).thenReturn(10L);
        when(storage.open(KEY)).thenReturn(new ByteArrayInputStream(new byte[]{9}));

        assertThat(service.open(1L, null).getAccessLevel()).isEqualTo("PROTECTED");
    }

    /**
     * ARCHIVED\uff1a\u533f\u540d\u62ff\u4e0d\u5230\uff1b\u7ba1\u7406\u5458\u53ef\u9884\u89c8。
     * 主要协作调用：<code>assetMapper.assetById</code>、<code>currentActorApi.currentOptional</code>、<code>service.open</code>、<code>storage.size</code>、<code>storage.open</code>。
     */
    @Test
    @DisplayName("ARCHIVED：匿名拿不到；管理员可预览")
    void archivedAccess() {
        when(assetMapper.assetById(1L)).thenReturn(asset("PUBLIC", MediaStatus.ARCHIVED_CODE, "IMAGE", KEY));
        when(currentActorApi.currentOptional()).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.open(1L, null))
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MEDIA_ARCHIVED");

        when(currentActorApi.currentOptional()).thenReturn(Optional.of(actorWith(MediaPermissions.READ)));
        when(storage.size(KEY)).thenReturn(8L);
        when(storage.open(KEY)).thenReturn(new ByteArrayInputStream(new byte[]{1}));
        assertThat(service.open(1L, null).getStream()).isNotNull();
    }

    /**
     * \u5a92\u4f53\u4e0d\u5b58\u5728\u6216 ID \u975e\u6cd5\u90fd\u62a5 MEDIA_NOT_FOUND。
     * 主要协作调用：<code>assetMapper.assetById</code>、<code>service.open</code>。
     */
    @Test
    @DisplayName("媒体不存在或 ID 非法都报 MEDIA_NOT_FOUND")
    void missingAsset() {
        when(assetMapper.assetById(404L)).thenReturn(null);

        for (Long id : new Long[]{404L, null, 0L}) {
            assertThatThrownBy(() -> service.open(id, null))
                    .as("id=%s", id)
                    .extracting(ex -> ((ApiException) ex).getCode())
                    .isEqualTo("MEDIA_NOT_FOUND");
        }
    }

    /**
     * Range \u8bf7\u6c42\u8fd4\u56de 206 \u8bed\u4e49\uff1a\u533a\u95f4\u88ab\u88c1\u526a\uff0c\u4e14\u53ea\u8bfb\u8be5\u533a\u95f4。
     * 主要协作调用：<code>assetMapper.assetById</code>、<code>storage.size</code>、<code>storage.openRange</code>、<code>service.open</code>。
     */
    @Test
    @DisplayName("Range 请求返回 206 语义：区间被裁剪，且只读该区间")
    void rangeRequestReturnsPartialContent() {
        when(assetMapper.assetById(1L)).thenReturn(asset("PUBLIC", MediaStatus.ACTIVE_CODE, "VIDEO", KEY));
        when(storage.size(KEY)).thenReturn(1000L);
        when(storage.openRange(KEY, 100L, 100L)).thenReturn(new ByteArrayInputStream(new byte[100]));

        MediaContentVO content = service.open(1L, "bytes=100-199");

        assertThat(content.partial()).isTrue();
        assertThat(content.contentLength()).isEqualTo(100L);
        assertThat(content.getRange().contentRange(1000L)).isEqualTo("bytes 100-199/1000");
        verify(storage).openRange(KEY, 100L, 100L);
    }

    /**
     * \u533a\u95f4\u4e0d\u53ef\u6ee1\u8db3\u65f6\u62a5 416\uff0c\u4e14\u4e0d\u4f1a\u6253\u5f00\u4efb\u4f55\u6d41。
     * 主要协作调用：<code>assetMapper.assetById</code>、<code>storage.size</code>、<code>service.open</code>。
     */
    @Test
    @DisplayName("区间不可满足时报 416，且不会打开任何流")
    void unsatisfiableRange() {
        when(assetMapper.assetById(1L)).thenReturn(asset("PUBLIC", MediaStatus.ACTIVE_CODE, "VIDEO", KEY));
        when(storage.size(KEY)).thenReturn(1000L);

        assertThatThrownBy(() -> service.open(1L, "bytes=5000-6000"))
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MEDIA_RANGE_INVALID");
        verify(storage, never()).open(anyString());
        verify(storage, never()).openRange(anyString(), org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong());
    }

    /**
     * \u6570\u636e\u5e93\u6709\u8bb0\u5f55\u4f46\u6587\u4ef6\u7f3a\u5931\u65f6\u62a5 MEDIA_CONTENT_MISSING。
     * 主要协作调用：<code>assetMapper.assetById</code>、<code>storage.size</code>、<code>service.open</code>。
     */
    @Test
    @DisplayName("数据库有记录但文件缺失时报 MEDIA_CONTENT_MISSING")
    void missingFileOnDisk() {
        when(assetMapper.assetById(1L)).thenReturn(asset("PUBLIC", MediaStatus.ACTIVE_CODE, "IMAGE", KEY));
        when(storage.size(KEY)).thenReturn(-1L);

        assertThatThrownBy(() -> service.open(1L, null))
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MEDIA_CONTENT_MISSING");
    }

    /**
     * \u5c55\u793a\u65b9\u5f0f\u6309\u7c7b\u578b\u533a\u5206\uff1a\u56fe\u7247/\u97f3\u9891/\u89c6\u9891 inline\uff0c\u6587\u6863\u4e0e\u538b\u7f29\u5305\u8d70\u4e0b\u8f7d。
     * 关键处理步骤调用 <code>readType</code>。
     */
    @Test
    @DisplayName("展示方式按类型区分：图片/音频/视频 inline，文档与压缩包走下载")
    void inlineDisplayByMediaType() {
        assertThat(readType("IMAGE").inlineDisplay()).isTrue();
        assertThat(readType("AUDIO").inlineDisplay()).isTrue();
        assertThat(readType("VIDEO").inlineDisplay()).isTrue();
        assertThat(readType("DOCUMENT").inlineDisplay()).isFalse();
        assertThat(readType("ARCHIVE").inlineDisplay()).isFalse();
    }

    /**
     * 读取类型。
     * 主要协作调用：<code>assetMapper.assetById</code>、<code>storage.size</code>、<code>storage.open</code>、<code>service.open</code>。
     *
     * @param mediaType 媒体类型；类型为 <code>String</code>
     * @return 类型为 <code>MediaContentVO</code> 的处理结果
     */
    private MediaContentVO readType(String mediaType) {
        MediaAssetEntity asset = asset("PUBLIC", MediaStatus.ACTIVE_CODE, mediaType, KEY);
        when(assetMapper.assetById(1L)).thenReturn(asset);
        when(storage.size(KEY)).thenReturn(1L);
        when(storage.open(KEY)).thenReturn(new ByteArrayInputStream(new byte[]{1}));
        return service.open(1L, null);
    }

    /**
     * 操作者包含处理入口。
     * 关键处理步骤调用 <code>Set.of</code>。
     *
     * @param permission 权限；类型为 <code>String</code>
     * @return 类型为 <code>CurrentActorApi.CurrentActor</code> 的处理结果
     */
    private static CurrentActorApi.CurrentActor actorWith(String permission) {
        return new CurrentActorApi.CurrentActor(1L, Set.of("ADMIN"), Set.of(permission));
    }

    /**
     * 资产处理入口。
     * 关键处理步骤调用 <code>entity.setId</code>、<code>entity.setOriginalName</code>、<code>entity.setMediaType</code>、<code>entity.setMimeType</code>。
     *
     * @param accessLevel 媒体访问级别；类型为 <code>String</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>
     * @param mediaType 媒体类型；类型为 <code>String</code>
     * @param key 键；类型为 <code>String</code>
     * @return 类型为 <code>MediaAssetEntity</code> 的处理结果
     */
    private static MediaAssetEntity asset(String accessLevel, String status, String mediaType, String key) {
        MediaAssetEntity entity = new MediaAssetEntity();
        entity.setId(1L);
        entity.setOriginalName("sample.bin");
        entity.setMediaType(mediaType);
        entity.setMimeType("application/octet-stream");
        entity.setSizeBytes(1000L);
        entity.setSha256("b".repeat(64));
        entity.setStorageProvider("LOCAL");
        entity.setStorageKey(key);
        entity.setAccessLevel(accessLevel);
        entity.setStatus(status);
        entity.setUploadedByAccountId(1L);
        return entity;
    }
}
