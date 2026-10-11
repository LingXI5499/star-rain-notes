/*
 * MediaReferenceVO：媒体模块的响应数据文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 * 主要协作类型：MediaReferenceEntity。
 * 阅读入口：from（从输入构造媒体引用）。
 */

package com.starrainnotes.media.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
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
/**
 * 媒体引用响应对象，属于媒体模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaReferenceVO {

    /**
     * 当前业务对象的数据库标识。
     * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    /**
     * 关联的媒体资产标识。
     * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long mediaAssetId;
    /**
     * 引用或数据来源所属模块。
     */
    private String sourceModule;
    /**
     * 来源对象的业务类型。
     */
    private String sourceType;
    /**
     * 来源对象的业务标识。
     * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long sourceId;
    /**
     * usage代码。
     */
    private String usageCode;
    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 从输入构造媒体引用。
     *
     * @param entity 数据库映射实体；类型为 <code>MediaReferenceEntity</code>
     * @return 由输入对象组装的媒体引用响应对象
     */
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
