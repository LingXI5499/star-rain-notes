package com.starrainnotes.media.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 模块间媒体摘要。
 * 只包含展示所需字段，刻意不含 storageKey 与 storageProvider：
 * 业务模块没有理由知道文件存在哪里。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaAssetSummary {

    private Long id;
    private String originalName;
    private String mediaType;
    private String mimeType;
    private Integer width;
    private Integer height;
    private String accessLevel;
    private String status;
    private String contentUrl;
}
