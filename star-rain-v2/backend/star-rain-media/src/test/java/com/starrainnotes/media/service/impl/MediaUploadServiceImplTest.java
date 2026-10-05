package com.starrainnotes.media.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.media.TestMediaFiles;
import com.starrainnotes.media.properties.MediaProperties;
import com.starrainnotes.media.entity.MediaAssetEntity;
import com.starrainnotes.media.enumeration.MediaAccessLevel;
import com.starrainnotes.media.enumeration.MediaStatus;
import com.starrainnotes.media.mapper.MediaAssetMapper;
import com.starrainnotes.media.service.MediaUploadLimiter;
import com.starrainnotes.media.storage.MediaStorage;
import com.starrainnotes.media.storage.StorageWriteCommand;
import com.starrainnotes.media.storage.StoredObject;
import com.starrainnotes.media.vo.MediaAssetVO;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

/*
 * MED-001 一致性测试。
 *
 * 重点验证两条路径：
 *   图片   —— 整份读入并落库宽高；
 *   其他   —— 流式落盘并同步计算 SHA-256（视频不能整份进内存）。
 * 以及“写库失败 → 补偿删除文件”这条无法靠事务覆盖的路径。
 */
@ExtendWith(MockitoExtension.class)
class MediaUploadServiceImplTest {

    @Mock
    private MediaStorage storage;

    @Mock
    private MediaAssetMapper assetMapper;

    @Mock
    private CurrentActorApi currentActorApi;

    @Mock
    private MediaUploadLimiter uploadLimiter;

    private MediaUploadServiceImpl service;

    @BeforeEach
    void setUp() {
        MediaUploadValidatorImpl validator = new MediaUploadValidatorImpl(new MediaProperties());
        service = new MediaUploadServiceImpl(validator, storage, assetMapper, currentActorApi, uploadLimiter);
    }

    @Test
    @DisplayName("图片上传：解析宽高入库，上传人取自认证上下文")
    void uploadsImageWithDimensions() throws IOException {
        when(currentActorApi.current()).thenReturn(actor());
        when(storage.store(any(StorageWriteCommand.class)))
                .thenReturn(new StoredObject("LOCAL", "2026/10/pic.png", 100L));
        AtomicReference<MediaAssetEntity> inserted = new AtomicReference<>();
        doAnswer(invocation -> {
            MediaAssetEntity entity = invocation.getArgument(0);
            entity.setId(9L);
            inserted.set(entity);
            return null;
        }).when(assetMapper).insertAsset(any(MediaAssetEntity.class));
        when(assetMapper.countBySha256(any())).thenReturn(1L);
        when(assetMapper.assetById(9L)).thenAnswer(invocation -> inserted.get());

        MediaAssetVO result = service.upload(
                new MockMultipartFile("file", "cover.png", "image/png", TestMediaFiles.png(120, 80)),
                MediaAccessLevel.PUBLIC);

        assertThat(result.getId()).isEqualTo(9L);
        assertThat(result.getAccessLevel()).isEqualTo("PUBLIC");
        assertThat(result.getStatus()).isEqualTo(MediaStatus.ACTIVE_CODE);
        assertThat(inserted.get().getWidth()).isEqualTo(120);
        assertThat(inserted.get().getHeight()).isEqualTo(80);
        assertThat(inserted.get().getSha256()).hasSize(64);
        assertThat(inserted.get().getUploadedByAccountId()).isEqualTo(42L);
        verify(uploadLimiter).check(42L);
        verify(storage, never()).delete(any());
    }

    @Test
    @DisplayName("视频上传：流式落盘，不落库宽高，仍然算出正确的 SHA-256")
    void uploadsVideoByStreaming() throws IOException {
        when(currentActorApi.current()).thenReturn(actor());
        byte[] video = TestMediaFiles.mp4();
        when(storage.store(any(StorageWriteCommand.class)))
                .thenAnswer(invocation -> {
                    // 模拟存储实现把流读完，从而触发摘要计算
                    StorageWriteCommand command = invocation.getArgument(0);
                    try (InputStream in = command.getContent()) {
                        assertThat(in.readAllBytes()).isEqualTo(video);
                    }
                    return new StoredObject("LOCAL", "2026/10/clip.mp4", (long) video.length);
                });
        AtomicReference<MediaAssetEntity> inserted = new AtomicReference<>();
        doAnswer(invocation -> {
            MediaAssetEntity entity = invocation.getArgument(0);
            entity.setId(11L);
            inserted.set(entity);
            return null;
        }).when(assetMapper).insertAsset(any(MediaAssetEntity.class));
        when(assetMapper.assetById(11L)).thenAnswer(invocation -> inserted.get());

        service.upload(new MockMultipartFile("file", "clip.mp4", "video/mp4", video),
                MediaAccessLevel.PUBLIC);

        assertThat(inserted.get().getMediaType()).isEqualTo("VIDEO");
        assertThat(inserted.get().getWidth()).isNull();
        assertThat(inserted.get().getHeight()).isNull();
        assertThat(inserted.get().getSha256()).hasSize(64);
        assertThat(inserted.get().getSizeBytes()).isEqualTo(video.length);
    }

    @Test
    @DisplayName("未指定访问级别时默认 PUBLIC：媒体服务于公开内容，默认不公开会让前台图片 403")
    void defaultsToPublic() throws IOException {
        when(currentActorApi.current()).thenReturn(actor());
        when(storage.store(any(StorageWriteCommand.class)))
                .thenReturn(new StoredObject("LOCAL", "2026/10/pic.png", 10L));
        AtomicReference<MediaAssetEntity> inserted = new AtomicReference<>();
        doAnswer(invocation -> {
            MediaAssetEntity entity = invocation.getArgument(0);
            entity.setId(10L);
            inserted.set(entity);
            return null;
        }).when(assetMapper).insertAsset(any(MediaAssetEntity.class));
        when(assetMapper.assetById(10L)).thenAnswer(invocation -> inserted.get());

        assertThat(service.upload(
                new MockMultipartFile("file", "cover.png", "image/png", TestMediaFiles.png(4, 4)), null)
                .getAccessLevel()).isEqualTo("PUBLIC");
    }

    @Test
    @DisplayName("写库失败时补偿删除已落盘文件，并向上抛出原始错误")
    void compensatesFileWhenDatabaseWriteFails() throws IOException {
        when(currentActorApi.current()).thenReturn(actor());
        when(storage.store(any(StorageWriteCommand.class)))
                .thenReturn(new StoredObject("LOCAL", "2026/10/orphan.png", 10L));
        doThrow(new ApiException("MEDIA_STORAGE_WRITE_FAILED", "数据库错误", 500))
                .when(assetMapper).insertAsset(any(MediaAssetEntity.class));

        assertThatThrownBy(() -> service.upload(
                new MockMultipartFile("file", "cover.png", "image/png", TestMediaFiles.png(4, 4)),
                MediaAccessLevel.PUBLIC))
                .isInstanceOf(ApiException.class);

        verify(storage).delete("2026/10/orphan.png");
    }

    @Test
    @DisplayName("元数据校验失败时不落盘：不产生需要补偿的文件")
    void doesNotStoreWhenMetadataInvalid() {
        when(currentActorApi.current()).thenReturn(actor());

        assertThatThrownBy(() -> service.upload(
                new MockMultipartFile("file", "icon.svg", "image/svg+xml", "<svg/>".getBytes()),
                MediaAccessLevel.PUBLIC))
                .isInstanceOf(ApiException.class);

        verify(storage, never()).store(any());
    }

    @Test
    @DisplayName("内容与扩展名不符时不落盘：图片走整份内容校验")
    void doesNotStoreWhenImageContentMismatches() {
        when(currentActorApi.current()).thenReturn(actor());

        assertThatThrownBy(() -> service.upload(
                new MockMultipartFile("file", "fake.png", "image/png", TestMediaFiles.mp4()),
                MediaAccessLevel.PUBLIC))
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MEDIA_CONTENT_TYPE_MISMATCH");

        verify(storage, never()).store(any());
    }

    @Test
    @DisplayName("流式路径下内容与扩展名不符时不落盘：只读文件头即可判定")
    void doesNotStoreWhenStreamedContentMismatches() throws IOException {
        when(currentActorApi.current()).thenReturn(actor());

        assertThatThrownBy(() -> service.upload(
                new MockMultipartFile("file", "fake.mp4", "video/mp4", TestMediaFiles.pdf()),
                MediaAccessLevel.PUBLIC))
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MEDIA_CONTENT_TYPE_MISMATCH");

        verify(storage, never()).store(any());
    }

    private static CurrentActorApi.CurrentActor actor() {
        return new CurrentActorApi.CurrentActor(42L, Set.of("ADMIN"), Set.of("media:upload"));
    }
}
