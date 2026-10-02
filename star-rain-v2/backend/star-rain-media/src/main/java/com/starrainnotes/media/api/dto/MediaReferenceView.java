package com.starrainnotes.media.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 模块间媒体引用视图。业务模块只拿到这个，拿不到 Media 的 Entity 与 Mapper
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaReferenceView {

    private Long mediaAssetId;
    private String sourceModule;
    private String sourceType;
    private Long sourceId;
    private String usageCode;
}
