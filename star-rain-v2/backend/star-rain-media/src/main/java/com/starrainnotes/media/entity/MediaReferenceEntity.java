package com.starrainnotes.media.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/*
 * 跨业务模块媒体引用。
 *
 * Media 不理解 Blog / Tutorial 的发布逻辑，只保存稳定的引用事实。
 * 唯一键 (media_asset_id, source_module, source_type, source_id, usage_code)
 * 保证同一用途不会被重复登记。
 */
@Data
@TableName("sr_media_reference")
public class MediaReferenceEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long mediaAssetId;

    // 来源模块，例如 BLOG / TUTORIAL / PORTFOLIO / PROFILE / SITE
    private String sourceModule;

    // 业务对象类型，例如 POST / CHAPTER / WORK / PROFILE
    private String sourceType;

    private Long sourceId;

    // 业务用途，例如 blog.cover；前缀必须与 sourceModule 一致
    private String usageCode;

    private LocalDateTime createdAt;
}
