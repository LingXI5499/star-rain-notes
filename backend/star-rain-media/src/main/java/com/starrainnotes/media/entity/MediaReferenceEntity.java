/*
 * MediaReferenceEntity：媒体模块的持久化实体文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */

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
/**
 * 媒体引用实体，属于媒体模块。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */
@Data
@TableName("sr_media_reference")
public class MediaReferenceEntity {

    /**
     * 当前业务对象的数据库标识。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联的媒体资产标识。
     */
    private Long mediaAssetId;

    // 来源模块，例如 BLOG / TUTORIAL / PORTFOLIO / PROFILE / SITE
    /**
     * 引用或数据来源所属模块。
     */
    private String sourceModule;

    // 业务对象类型，例如 POST / CHAPTER / WORK / PROFILE
    /**
     * 来源对象的业务类型。
     */
    private String sourceType;

    /**
     * 来源对象的业务标识。
     */
    private Long sourceId;

    // 业务用途，例如 blog.cover；前缀必须与 sourceModule 一致
    /**
     * usage代码。
     */
    private String usageCode;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
}
