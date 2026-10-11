package com.starrainnotes.media.service.impl;

import com.starrainnotes.media.exception.MediaContentTypeMismatchException;
import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.media.entity.MediaAssetEntity;
import com.starrainnotes.media.mapper.MediaAssetMapper;
import com.starrainnotes.media.service.MediaUploadService;
import com.starrainnotes.media.security.MediaUploadLimiter;
import com.starrainnotes.media.service.MediaUploadValidator;
import com.starrainnotes.media.storage.MediaStorage;
import com.starrainnotes.media.dto.MediaStorageResultDTO;
import com.starrainnotes.media.utils.FileSignatures;
import com.starrainnotes.media.enumeration.MediaAccessLevel;
import com.starrainnotes.media.enumeration.MediaStatus;
import com.starrainnotes.media.dto.UploadMetadata;
import com.starrainnotes.media.vo.MediaAssetVO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/*
 * MED-001 上传实现。
 *
 * 文件系统与 MySQL 不能共享同一个事务，因此采用
 * 「先落盘 → 再写库 → 写库失败补偿删除」：
 *   校验 → Storage.store() → INSERT MediaAsset → 成功
 *   数据库写入失败 → catch → Storage.delete(storageKey)
 * 进程异常宕机留下的孤儿文件由 MediaCleanupService 兜底。
 *
 * 内存策略（关键）：
 *   图片   —— 整份读入，因为要解析宽高（图片上限 10MB，可接受）；
 *   其他   —— 只读文件头做 magic bytes 校验，其余流式落盘，
 *             并在同一遍读取中用 DigestInputStream 计算 SHA-256。
 * 因此 200MB 的视频不会占用等量堆内存，也不会被读两遍。
 */
@Service
public class MediaUploadServiceImpl implements MediaUploadService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaUploadServiceImpl.class);

    private final MediaUploadValidator validator;
    private final MediaStorage storage;
    private final MediaAssetMapper assetMapper;
    private final CurrentActorApi currentActorApi;
    private final MediaUploadLimiter uploadLimiter;

    public MediaUploadServiceImpl(MediaUploadValidator validator,
                                  MediaStorage storage,
                                  MediaAssetMapper assetMapper,
                                  CurrentActorApi currentActorApi,
                                  MediaUploadLimiter uploadLimiter) {
        this.validator = validator;
        this.storage = storage;
        this.assetMapper = assetMapper;
        this.currentActorApi = currentActorApi;
        this.uploadLimiter = uploadLimiter;
    }

    @Override
    @Transactional
    public MediaAssetVO upload(MultipartFile file, MediaAccessLevel accessLevel) {
        // 上传人来自认证上下文，不接受前端提交，避免伪造归属
        Long uploaderAccountId = currentActorApi.current().getAccountId();
        uploadLimiter.check(uploaderAccountId);
        UploadMetadata metadata = validator.validate(file);
        MediaAccessLevel level = accessLevel == null ? MediaAccessLevel.PUBLIC : accessLevel;

        StoredFile storedFile = storeAndDigest(file, metadata);

        try {
            MediaAssetEntity entity = new MediaAssetEntity();
            entity.setOriginalName(metadata.getOriginalName());
            entity.setMediaType(metadata.mediaType().name());
            entity.setMimeType(metadata.mimeType());
            entity.setFileExtension(metadata.getFileExtension());
            entity.setSizeBytes(storedFile.getStored().getSizeBytes());
            entity.setSha256(storedFile.getSha256());
            entity.setStorageProvider(storedFile.getStored().getStorageProvider());
            entity.setStorageKey(storedFile.getStored().getStorageKey());
            entity.setWidth(storedFile.getWidth());
            entity.setHeight(storedFile.getHeight());
            entity.setAccessLevel(level.name());
            entity.setStatus(MediaStatus.ACTIVE_CODE);
            entity.setUploadedByAccountId(uploaderAccountId);
            assetMapper.insertAsset(entity);

            // sha256 不是业务唯一键：这里只回传同内容素材数量用于提示，不静默合并
            long sameSha256Count = assetMapper.countBySha256(storedFile.getSha256());
            return MediaAssetVO.from(assetMapper.assetById(entity.getId()), sameSha256Count);
        } catch (RuntimeException ex) {
            compensateStoredFile(storedFile.getStored().getStorageKey(), ex);
            throw ex;
        }
    }

    /*
     * 落盘并计算摘要。图片走整份读入，其余走“头部校验 + 流式复制”。
     */
    private StoredFile storeAndDigest(MultipartFile file, UploadMetadata metadata) {
        if (metadata.requiresFullContent()) {
            return storeWholeImage(file, metadata);
        }
        return storeStreamed(file, metadata);
    }

    private StoredFile storeWholeImage(MultipartFile file, UploadMetadata metadata) {
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException ex) {
            throw new MediaContentTypeMismatchException();
        }
        int[] dimensions = verifyContent(metadata, content);
        MediaStorageResultDTO stored = storage.store(
                new ByteArrayInputStream(content), metadata.getFileExtension());
        return new StoredFile(stored, sha256Hex(content),
                dimensions == null ? null : dimensions[0],
                dimensions == null ? null : dimensions[1]);
    }

    private StoredFile storeStreamed(MultipartFile file, UploadMetadata metadata) {
        MessageDigest digest = newSha256();
        try (InputStream source = file.getInputStream()) {
            // 只需要文件头就能完成签名校验；其余字节不进入堆内存
            byte[] head = source.readNBytes(FileSignatures.HEAD_BYTES);
            verifyContent(metadata, head);

            try (DigestInputStream digesting = new DigestInputStream(
                    new SequenceInputStream(new ByteArrayInputStream(head), source), digest)) {
                MediaStorageResultDTO stored = storage.store(digesting, metadata.getFileExtension());
                return new StoredFile(stored, HexFormat.of().formatHex(digest.digest()), null, null);
            }
        } catch (IOException ex) {
            throw new MediaContentTypeMismatchException();
        }
    }

    private int[] verifyContent(UploadMetadata metadata, byte[] content) {
        try {
            return FileSignatures.verify(metadata.getFileType(), content);
        } catch (IOException ex) {
            // 扩展名、声明类型都对，但真实内容不是这个格式
            throw new MediaContentTypeMismatchException();
        }
    }

    // 补偿删除失败不能掩盖原始异常，只记录日志，交由孤儿清理任务兜底
    private void compensateStoredFile(String storageKey, RuntimeException cause) {
        try {
            storage.delete(storageKey);
        } catch (RuntimeException compensationFailure) {
            LOGGER.error("数据库写入失败后补偿删除媒体文件也失败，storageKey={}（将由孤儿清理任务兜底）",
                    storageKey, compensationFailure);
        }
        LOGGER.warn("媒体数据库写入失败，已回滚存储文件，storageKey={}", storageKey, cause);
    }

    private static MessageDigest newSha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private static String sha256Hex(byte[] content) {
        return HexFormat.of().formatHex(newSha256().digest(content));
    }

    // 落盘结果 + 摘要 + 图片宽高（非图片为 null）
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class StoredFile {

        private MediaStorageResultDTO stored;
        private String sha256;
        private Integer width;
        private Integer height;
    }
}
