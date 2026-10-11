/*
 * MediaReferenceCommand：媒体模块的请求与传输数据文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.media.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 建立 / 解除媒体引用的调用参数。
 *
 * 这是模块间调用契约，不开放给浏览器：
 * 业务模块用自己的 Service 组装该命令，浏览器无法通过任何公开接口直接提交
 * sourceModule / sourceId 来伪造跨模块引用。
 *
 * mediaAssetId —— 媒体资产 ID
 * sourceModule —— 来源模块，例如 BLOG / TUTORIAL / PORTFOLIO / PROFILE / SITE
 * sourceType   —— 业务对象类型，例如 POST / CHAPTER / WORK / PROFILE
 * sourceId     —— 来源业务对象 ID
 * usageCode    —— 业务用途，例如 blog.cover；必须以 sourceModule 的小写前缀开头
 */
/**
 * 媒体引用Command，属于媒体模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaReferenceCommand {

    /**
     * 关联的媒体资产标识。
     */
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
     */
    private Long sourceId;
    /**
     * usage代码。
     */
    private String usageCode;
}
