/*
 * MediaAssetEntity：媒体模块的持久化实体文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 * 主要协作类型：MediaType。
 */

package com.starrainnotes.media.entity;

import com.starrainnotes.media.enumeration.MediaType;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/*
 * 媒体资产：系统中的稳定媒体身份。
 *
 * 业务模块只保存 mediaAssetId，禁止把服务器绝对路径当作业务身份。
 * storageKey 是相对 Key（形如 2026/10/<uuid>.png），绝对根路径只存在于配置中。
 */
/**
 * 媒体资产实体，属于媒体模块。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */
@Data
@TableName("sr_media_asset")
public class MediaAssetEntity {

    /**
     * 当前业务对象的数据库标识。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    // 用户上传时的原始文件名，允许重复；真实存储名由 Storage 生成
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
     * 文件扩展名。
     */
    private String fileExtension;

    /**
     * 文件大小，单位为字节。
     */
    private Long sizeBytes;

    // 文件内容 SHA-256：用于重复提示与完整性校验，不是业务唯一键
    /**
     * 文件内容的 SHA-256 摘要。
     */
    private String sha256;

    /**
     * 存储提供方。
     */
    private String storageProvider;
    /**
     * 媒体在存储根目录内的相对键。
     */
    private String storageKey;

    // 仅图片有值
    /**
     * 宽度。
     */
    private Integer width;
    /**
     * 高度。
     */
    private Integer height;

    // PUBLIC / PROTECTED
    /**
     * 媒体访问级别。
     */
    private String accessLevel;

    // ACTIVE / ARCHIVED
    /**
     * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束。
     */
    private String status;

    /**
     * 上传文件的账号标识，来自认证上下文。
     */
    private Long uploadedByAccountId;

    /**
     * 已归档时间。
     */
    private LocalDateTime archivedAt;
    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
