package com.starrainnotes.media.vo;

import com.starrainnotes.media.entity.MediaAssetEntity;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 媒体资产详情视图：在列表字段基础上补充 referenceCount。
 *
 * MED-007 需要回答“这个媒体正在被谁引用”：这里只给数量，
 * 引用明细由 GET /api/admin/media/assets/{id}/references 提供。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaAssetDetailVO {

    private Long id;
    private String originalName;
    private String mediaType;
    private String mimeType;
    private String fileExtension;
    private Long sizeBytes;
    private String sha256;
    private Integer width;
    private Integer height;
    private String accessLevel;
    private String status;
    private String storageProvider;
    private Long uploadedByAccountId;
    private LocalDateTime archivedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String contentUrl;
    private long referenceCount;

    public static MediaAssetDetailVO from(MediaAssetEntity entity, long referenceCount) {
        return MediaAssetDetailVO.builder()
                .id(entity.getId())
                .originalName(entity.getOriginalName())
                .mediaType(entity.getMediaType())
                .mimeType(entity.getMimeType())
                .fileExtension(entity.getFileExtension())
                .sizeBytes(entity.getSizeBytes())
                .sha256(entity.getSha256())
                .width(entity.getWidth())
                .height(entity.getHeight())
                .accessLevel(entity.getAccessLevel())
                .status(entity.getStatus())
                .storageProvider(entity.getStorageProvider())
                .uploadedByAccountId(entity.getUploadedByAccountId())
                .archivedAt(entity.getArchivedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .contentUrl(MediaAssetVO.contentUrl(entity.getId()))
                .referenceCount(referenceCount)
                .build();
    }
}
