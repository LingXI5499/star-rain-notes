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
@Data
@TableName("sr_media_asset")
public class MediaAssetEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 用户上传时的原始文件名，允许重复；真实存储名由 Storage 生成
    private String originalName;

    private String mediaType;
    private String mimeType;
    private String fileExtension;

    private Long sizeBytes;

    // 文件内容 SHA-256：用于重复提示与完整性校验，不是业务唯一键
    private String sha256;

    private String storageProvider;
    private String storageKey;

    // 仅图片有值
    private Integer width;
    private Integer height;

    // PUBLIC / PROTECTED
    private String accessLevel;

    // ACTIVE / ARCHIVED
    private String status;

    private Long uploadedByAccountId;

    private LocalDateTime archivedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
