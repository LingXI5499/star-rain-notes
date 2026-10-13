/*
 * MediaUploadServiceImplTest：媒体模块的测试文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：CurrentActorApi、ApiException、TestMediaFiles、MediaProperties、MediaAssetEntity、MediaAccessLevel、MediaStatus。
 * 阅读入口：getBytes（读取字节）。
 */

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
import com.starrainnotes.media.security.MediaUploadLimiter;
import com.starrainnotes.media.storage.MediaStorage;
import com.starrainnotes.media.dto.MediaStorageResultDTO;
import com.starrainnotes.media.vo.MediaAssetVO;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
/**
 * 媒体上传业务服务实现测试，属于媒体模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
@ExtendWith(MockitoExtension.class)
class MediaUploadServiceImplTest {
    /** A rollback discovered after upload returns still compensates the physical file. */
    @Test
    void compensatesAfterTransactionRollback() throws IOException {
        when(currentActorApi.current()).thenReturn(actor());
        when(storage.store(any(InputStream.class), any()))
                .thenReturn(new MediaStorageResultDTO("LOCAL", "2026/10/rollback.png", 100L));
        AtomicReference<MediaAssetEntity> inserted = new AtomicReference<>();
        doAnswer(call -> {
            MediaAssetEntity entity = call.getArgument(0); entity.setId(99L); inserted.set(entity); return 1;
        }).when(assetMapper).insertAsset(any());
        when(assetMapper.assetById(99L)).thenAnswer(call -> inserted.get());
        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
        try {
            service.upload(new MockMultipartFile("file", "cover.png", "image/png", TestMediaFiles.png(4, 4)), MediaAccessLevel.PUBLIC);
            verify(storage, never()).delete(any());
            var callbacks = org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations();
            assertThat(callbacks).isNotEmpty();
            callbacks.forEach(callback -> callback.afterCompletion(org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK));
            verify(storage).delete("2026/10/rollback.png");
        } finally { org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization(); }
    }

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
     * 提供当前认证操作者身份的跨模块接口。
     * 协作类型：<code>CurrentActorApi</code>。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock
    private CurrentActorApi currentActorApi;

    /**
     * 上传Limiter。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock
    private MediaUploadLimiter uploadLimiter;

    /**
     * 业务服务。
     */
    private MediaUploadServiceImpl service;

    /**
     * 初始化当前测试场景的被测对象与模拟依赖。
     */
    @BeforeEach
    void setUp() {
        MediaUploadValidatorImpl validator = new MediaUploadValidatorImpl(new MediaProperties());
        service = new MediaUploadServiceImpl(validator, storage, assetMapper, currentActorApi, uploadLimiter);
    }

    /**
     * \u56fe\u7247\u4e0a\u4f20\uff1a\u89e3\u6790\u5bbd\u9ad8\u5165\u5e93\uff0c\u4e0a\u4f20\u4eba\u53d6\u81ea\u8ba4\u8bc1\u4e0a\u4e0b\u6587。
     * 主要协作调用：<code>currentActorApi.current</code>、<code>storage.store</code>、<code>assetMapper.countBySha256</code>、<code>assetMapper.assetById</code>、<code>service.upload</code>。
     * @throws IOException 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test
    @DisplayName("图片上传：解析宽高入库，上传人取自认证上下文")
    void uploadsImageWithDimensions() throws IOException {
        when(currentActorApi.current()).thenReturn(actor());
        when(storage.store(any(InputStream.class), any()))
                .thenReturn(new MediaStorageResultDTO("LOCAL", "2026/10/pic.png", 100L));
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

    /**
     * \u89c6\u9891\u4e0a\u4f20\uff1a\u6d41\u5f0f\u843d\u76d8\uff0c\u4e0d\u843d\u5e93\u5bbd\u9ad8\uff0c\u4ecd\u7136\u7b97\u51fa\u6b63\u786e\u7684 SHA-256。
     * 主要协作调用：<code>currentActorApi.current</code>、<code>storage.store</code>、<code>assetMapper.assetById</code>、<code>service.upload</code>。
     * @throws IOException 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test
    @DisplayName("视频上传：流式落盘，不落库宽高，仍然算出正确的 SHA-256")
    void uploadsVideoByStreaming() throws IOException {
        when(currentActorApi.current()).thenReturn(actor());
        byte[] video = TestMediaFiles.mp4();
        when(storage.store(any(InputStream.class), any()))
                .thenAnswer(invocation -> {
                    // 模拟存储实现把流读完，从而触发摘要计算
                    try (InputStream in = invocation.getArgument(0)) {
                        assertThat(in.readAllBytes()).isEqualTo(video);
                    }
                    return new MediaStorageResultDTO("LOCAL", "2026/10/clip.mp4", (long) video.length);
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

    /**
     * \u672a\u6307\u5b9a\u8bbf\u95ee\u7ea7\u522b\u65f6\u9ed8\u8ba4 PUBLIC\uff1a\u5a92\u4f53\u670d\u52a1\u4e8e\u516c\u5f00\u5185\u5bb9\uff0c\u9ed8\u8ba4\u4e0d\u516c\u5f00\u4f1a\u8ba9\u524d\u53f0\u56fe\u7247 403。
     * 主要协作调用：<code>currentActorApi.current</code>、<code>storage.store</code>、<code>assetMapper.assetById</code>、<code>service.upload</code>。
     * @throws IOException 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test
    @DisplayName("未指定访问级别时默认 PUBLIC：媒体服务于公开内容，默认不公开会让前台图片 403")
    void defaultsToPublic() throws IOException {
        when(currentActorApi.current()).thenReturn(actor());
        when(storage.store(any(InputStream.class), any()))
                .thenReturn(new MediaStorageResultDTO("LOCAL", "2026/10/pic.png", 10L));
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

    /**
     * \u5199\u5e93\u5931\u8d25\u65f6\u8865\u507f\u5220\u9664\u5df2\u843d\u76d8\u6587\u4ef6\uff0c\u5e76\u5411\u4e0a\u629b\u51fa\u539f\u59cb\u9519\u8bef。
     * 主要协作调用：<code>currentActorApi.current</code>、<code>storage.store</code>、<code>service.upload</code>。
     * @throws IOException 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test
    @DisplayName("写库失败时补偿删除已落盘文件，并向上抛出原始错误")
    void compensatesFileWhenDatabaseWriteFails() throws IOException {
        when(currentActorApi.current()).thenReturn(actor());
        when(storage.store(any(InputStream.class), any()))
                .thenReturn(new MediaStorageResultDTO("LOCAL", "2026/10/orphan.png", 10L));
        doThrow(new ApiException("MEDIA_STORAGE_WRITE_FAILED", "数据库错误", 500))
                .when(assetMapper).insertAsset(any(MediaAssetEntity.class));

        assertThatThrownBy(() -> service.upload(
                new MockMultipartFile("file", "cover.png", "image/png", TestMediaFiles.png(4, 4)),
                MediaAccessLevel.PUBLIC))
                .isInstanceOf(ApiException.class);

        verify(storage).delete("2026/10/orphan.png");
    }

    /**
     * \u5143\u6570\u636e\u6821\u9a8c\u5931\u8d25\u65f6\u4e0d\u843d\u76d8\uff1a\u4e0d\u4ea7\u751f\u9700\u8981\u8865\u507f\u7684\u6587\u4ef6。
     * 主要协作调用：<code>currentActorApi.current</code>、<code>service.upload</code>。
     */
    @Test
    @DisplayName("元数据校验失败时不落盘：不产生需要补偿的文件")
    void doesNotStoreWhenMetadataInvalid() {
        when(currentActorApi.current()).thenReturn(actor());

        assertThatThrownBy(() -> service.upload(
                new MockMultipartFile("file", "icon.svg", "image/svg+xml", "<svg/>".getBytes()),
                MediaAccessLevel.PUBLIC))
                .isInstanceOf(ApiException.class);

        verify(storage, never()).store(any(), any());
    }

    /**
     * \u5185\u5bb9\u4e0e\u6269\u5c55\u540d\u4e0d\u7b26\u65f6\u4e0d\u843d\u76d8\uff1a\u56fe\u7247\u8d70\u6574\u4efd\u5185\u5bb9\u6821\u9a8c。
     * 主要协作调用：<code>currentActorApi.current</code>、<code>service.upload</code>。
     */
    @Test
    @DisplayName("内容与扩展名不符时不落盘：图片走整份内容校验")
    void doesNotStoreWhenImageContentMismatches() {
        when(currentActorApi.current()).thenReturn(actor());

        assertThatThrownBy(() -> service.upload(
                new MockMultipartFile("file", "fake.png", "image/png", TestMediaFiles.mp4()),
                MediaAccessLevel.PUBLIC))
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MEDIA_CONTENT_TYPE_MISMATCH");

        verify(storage, never()).store(any(), any());
    }

    /**
     * \u6d41\u5f0f\u8def\u5f84\u4e0b\u5185\u5bb9\u4e0e\u6269\u5c55\u540d\u4e0d\u7b26\u65f6\u4e0d\u843d\u76d8\uff1a\u53ea\u8bfb\u6587\u4ef6\u5934\u5373\u53ef\u5224\u5b9a。
     * 主要协作调用：<code>currentActorApi.current</code>、<code>service.upload</code>。
     * @throws IOException 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test
    @DisplayName("流式路径下内容与扩展名不符时不落盘：只读文件头即可判定")
    void doesNotStoreWhenStreamedContentMismatches() throws IOException {
        when(currentActorApi.current()).thenReturn(actor());

        assertThatThrownBy(() -> service.upload(
                new MockMultipartFile("file", "fake.mp4", "video/mp4", TestMediaFiles.pdf()),
                MediaAccessLevel.PUBLIC))
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MEDIA_CONTENT_TYPE_MISMATCH");

        verify(storage, never()).store(any(), any());
    }

    /**
     * \u4e2d\u6587\u6587\u672c\u8de8\u8d8a 64KB \u6587\u4ef6\u5934\u8fb9\u754c\u65f6\u5b8c\u6574\u4e0a\u4f20\uff0c\u4e0d\u622a\u65ad\u6c49\u5b57\uff0c\u6458\u8981\u4e0e\u539f\u6587\u4e00\u81f4。
     * 主要协作调用：<code>currentActorApi.current</code>、<code>storage.store</code>、<code>assetMapper.assetById</code>、<code>service.upload</code>。
     * 实现中的显式失败类型包括 <code>IllegalStateException</code>；具体触发条件见对应分支。
     *
     * @param extension 扩展名；类型为 <code>String</code>
     * @throws NoSuchAlgorithmException 底层操作失败或输入不满足本方法要求时抛出
     */
    @ParameterizedTest
    @ValueSource(strings = {"md", "markdown", "txt"})
    @DisplayName("中文文本跨越 64KB 文件头边界时完整上传，不截断汉字，摘要与原文一致")
    void uploadsTextAcrossHeaderBoundary(String extension) throws NoSuchAlgorithmException {
        when(currentActorApi.current()).thenReturn(actor());
        byte[] text = ("\uFEFF# 中文教程\n" + "汉".repeat(23800) + "🌧️\n")
                .getBytes(StandardCharsets.UTF_8);
        when(storage.store(any(InputStream.class), any())).thenAnswer(invocation -> {
            try (InputStream in = invocation.getArgument(0)) {
                assertThat(in.readAllBytes()).isEqualTo(text);
            }
            return new MediaStorageResultDTO("LOCAL", "2026/10/article." + extension, (long) text.length);
        });
        AtomicReference<MediaAssetEntity> inserted = new AtomicReference<>();
        doAnswer(invocation -> {
            MediaAssetEntity entity = invocation.getArgument(0);
            entity.setId(12L);
            inserted.set(entity);
            return null;
        }).when(assetMapper).insertAsset(any(MediaAssetEntity.class));
        when(assetMapper.assetById(12L)).thenAnswer(invocation -> inserted.get());

        MockMultipartFile file = new MockMultipartFile("file", "中文教程." + extension, "text/plain", text)
    /**
     * 当前调用使用的匿名协作实现，属于媒体模块。
     * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
     */
    {
            /**
             * 读取字节。
             * 实现中的显式失败类型包括 <code>IllegalStateException</code>；具体触发条件见对应分支。
             * @return 类型为 <code>byte[]</code> 的处理结果
             */
            @Override
            public byte[] getBytes() {
                throw new IllegalStateException("文本不能整份读入内存");
            }
        };
        MediaAssetVO uploaded = service.upload(file, MediaAccessLevel.PUBLIC);

        assertThat(uploaded.getSizeBytes()).isEqualTo(text.length);
        assertThat(inserted.get().getSha256()).isEqualTo(
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text)));
    }

    /**
     * \u6587\u672c\u6587\u4ef6\u5934\u4e4b\u540e\u5305\u542b NUL\u3001\u975e\u6cd5 UTF-8 \u6216\u672b\u5c3e\u6b8b\u7f3a\u6c49\u5b57\u65f6\uff0c\u6821\u9a8c\u5931\u8d25\u4e14\u4e0d\u843d\u76d8。
     * 主要协作调用：<code>currentActorApi.current</code>、<code>service.upload</code>。
     *
     * @param invalidByte 无效Byte；类型为 <code>int</code>
     */
    @ParameterizedTest
    @ValueSource(ints = {0, 255, 228})
    @DisplayName("文本文件头之后包含 NUL、非法 UTF-8 或末尾残缺汉字时，校验失败且不落盘")
    void rejectsInvalidTextAfterHeader(int invalidByte) {
        when(currentActorApi.current()).thenReturn(actor());
        byte[] text = ("a".repeat(70 * 1024) + "x").getBytes(StandardCharsets.UTF_8);
        text[text.length - 1] = (byte) invalidByte;

        assertThatThrownBy(() -> service.upload(
                new MockMultipartFile("file", "invalid.md", "text/markdown", text), MediaAccessLevel.PUBLIC))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("MEDIA_CONTENT_TYPE_MISMATCH");

        verify(storage, never()).store(any(), any());
        verify(assetMapper, never()).insertAsset(any());
    }

    /**
     * 当前操作对应的身份对象处理入口。
     * 关键处理步骤调用 <code>Set.of</code>。
     * @return 类型为 <code>CurrentActorApi.CurrentActor</code> 的处理结果
     */
    private static CurrentActorApi.CurrentActor actor() {
        return new CurrentActorApi.CurrentActor(42L, Set.of("ADMIN"), Set.of("media:upload"));
    }
}
