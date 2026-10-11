/*
 * MediaAssetSummary：媒体模块的请求与传输数据文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

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
/**
 * 媒体资产摘要，属于媒体模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaAssetSummary {

    /**
     * 当前业务对象的数据库标识。
     */
    private Long id;
    /**
     * 原始名称。
     */
    private String originalName;
    /**
     * 媒体类型。
     */
    private String mediaType;
    /**
     * 文件的 MIME 类型。
     */
    private String mimeType;
    /**
     * 宽度。
     */
    private Integer width;
    /**
     * 高度。
     */
    private Integer height;
    /**
     * 媒体访问级别。
     */
    private String accessLevel;
    /**
     * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束。
     */
    private String status;
    /**
     * 内容地址。
     */
    private String contentUrl;
}
