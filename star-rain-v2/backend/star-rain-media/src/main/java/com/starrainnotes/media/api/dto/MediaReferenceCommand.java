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
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaReferenceCommand {

    private Long mediaAssetId;
    private String sourceModule;
    private String sourceType;
    private Long sourceId;
    private String usageCode;
}
