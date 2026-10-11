/*
 * VocabularyWordAudioVO：英语学习模块的响应数据文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */

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
/**
 * 词汇单词音频响应对象，属于英语学习模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyWordAudioVO {

    /**
     * 当前业务对象的数据库标识。
     * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 单词标识。
     * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long wordId;

    /**
     * accent。
     */
    private String accent;

    /**
     * 关联的媒体资产标识。
     * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long mediaAssetId;

    /**
     * 公开地址。
     */
    private String publicUrl;

    /**
     * 提供方。
     */
    private String provider;

    /**
     * 来源地址。
     */
    private String sourceUrl;

    /**
     * licenseNote。
     */
    private String licenseNote;

    /**
     * primary。
     */
    private boolean primary;
}
