package com.starrainnotes.media.vo;

import com.starrainnotes.media.entity.MediaAssetEntity;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 媒体资产列表 / 上传响应视图。
 *
 * 不暴露 storageKey：存储定位属于模块内部实现，业务模块只保存 mediaAssetId。
 * sameSha256Count 只在重复检测时有意义，>1 表示库里已有同内容素材，
 * 前端可据此提示，但系统不会静默合并成同一条 MediaAsset。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaAssetVO {

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
    private String contentUrl;
    private long sameSha256Count;

    public static String contentUrl(Long id) {
        return id == null ? null : "/api/media/assets/" + id + "/content";
    }

    public static MediaAssetVO from(MediaAssetEntity entity, long sameSha256Count) {
        return MediaAssetVO.builder()
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
                .contentUrl(contentUrl(entity.getId()))
                .sameSha256Count(sameSha256Count)
                .build();
    }
}
