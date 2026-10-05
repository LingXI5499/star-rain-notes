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
@ExtendWith(MockitoExtension.class)
class MediaAccessServiceImplTest {

    private static final String KEY = "2026/10/1.mp4";

    @Mock
    private MediaAssetMapper assetMapper;

    @Mock
    private MediaStorage storage;

    @Mock
    private CurrentActorApi currentActorApi;

    @InjectMocks
    private MediaAccessServiceImpl service;

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

    @Test
    @DisplayName("ACTIVE + PROTECTED：具备 media:read 的管理员可读")
    void protectedActiveReadableWithPermission() {
        when(assetMapper.assetById(1L)).thenReturn(asset("PROTECTED", MediaStatus.ACTIVE_CODE, "IMAGE", KEY));
        when(currentActorApi.currentOptional()).thenReturn(Optional.of(actorWith(MediaPermissions.READ)));
        when(storage.size(KEY)).thenReturn(10L);
        when(storage.open(KEY)).thenReturn(new ByteArrayInputStream(new byte[]{9}));

        assertThat(service.open(1L, null).getAccessLevel()).isEqualTo("PROTECTED");
    }

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

    @Test
    @DisplayName("数据库有记录但文件缺失时报 MEDIA_CONTENT_MISSING")
    void missingFileOnDisk() {
        when(assetMapper.assetById(1L)).thenReturn(asset("PUBLIC", MediaStatus.ACTIVE_CODE, "IMAGE", KEY));
        when(storage.size(KEY)).thenReturn(-1L);

        assertThatThrownBy(() -> service.open(1L, null))
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MEDIA_CONTENT_MISSING");
    }

    @Test
    @DisplayName("展示方式按类型区分：图片/音频/视频 inline，文档与压缩包走下载")
    void inlineDisplayByMediaType() {
        assertThat(readType("IMAGE").inlineDisplay()).isTrue();
        assertThat(readType("AUDIO").inlineDisplay()).isTrue();
        assertThat(readType("VIDEO").inlineDisplay()).isTrue();
        assertThat(readType("DOCUMENT").inlineDisplay()).isFalse();
        assertThat(readType("ARCHIVE").inlineDisplay()).isFalse();
    }

    private MediaContentVO readType(String mediaType) {
        MediaAssetEntity asset = asset("PUBLIC", MediaStatus.ACTIVE_CODE, mediaType, KEY);
        when(assetMapper.assetById(1L)).thenReturn(asset);
        when(storage.size(KEY)).thenReturn(1L);
        when(storage.open(KEY)).thenReturn(new ByteArrayInputStream(new byte[]{1}));
        return service.open(1L, null);
    }

    private static CurrentActorApi.CurrentActor actorWith(String permission) {
        return new CurrentActorApi.CurrentActor(1L, Set.of("ADMIN"), Set.of(permission));
    }

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
