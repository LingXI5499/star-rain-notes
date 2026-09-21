package com.starrainnotes.media.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Media asset (frozen table {@code media_asset}). Storage paths are relative
 * keys used internally only and must never be exposed in responses.
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("media_asset")
public class MediaAsset {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String assetType;
    private String originalName;
    private String storedName;
    private String mimeType;
    private String extension;
    private Long sizeBytes;
    private String storagePath;
    private String publicUrl;
    private Integer width;
    private Integer height;
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
}
