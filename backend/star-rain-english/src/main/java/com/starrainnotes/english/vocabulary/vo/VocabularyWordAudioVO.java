package com.starrainnotes.english.vocabulary.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 单词授权发音音频。
 *
 * mediaAssetId 为 null 表示只有来源信息、V2 媒体库还没有对应资产（V1 导入后的常见状态）；
 * publicUrl 只在有媒体资产时给出，前端据此决定是否优先走「已上传的授权音频」。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyWordAudioVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long wordId;

    private String accent;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long mediaAssetId;

    private String publicUrl;

    private String provider;

    private String sourceUrl;

    private String licenseNote;

    private boolean primary;
}
