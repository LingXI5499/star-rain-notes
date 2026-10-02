package com.starrainnotes.media.vo;

import com.starrainnotes.media.entity.MediaReferenceEntity;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 媒体引用视图（模块内）。
 * 说明某个媒体被哪个业务对象的哪个用途引用，供后台“正在被谁引用”面板展示。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaReferenceVO {

    private Long id;
    private Long mediaAssetId;
    private String sourceModule;
    private String sourceType;
    private Long sourceId;
    private String usageCode;
    private LocalDateTime createdAt;

    public static MediaReferenceVO from(MediaReferenceEntity entity) {
        return MediaReferenceVO.builder()
                .id(entity.getId())
                .mediaAssetId(entity.getMediaAssetId())
                .sourceModule(entity.getSourceModule())
                .sourceType(entity.getSourceType())
                .sourceId(entity.getSourceId())
                .usageCode(entity.getUsageCode())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
