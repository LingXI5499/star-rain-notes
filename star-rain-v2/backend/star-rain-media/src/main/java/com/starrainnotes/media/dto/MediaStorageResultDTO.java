package com.starrainnotes.media.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 存储写入结果，只在存储实现与上传业务之间传递。
 *
 * storageKey 是相对 Key（形如 2026/10/<uuid>.png），禁止是服务器绝对路径。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaStorageResultDTO {

    private String storageProvider;
    private String storageKey;
    private long sizeBytes;
}
