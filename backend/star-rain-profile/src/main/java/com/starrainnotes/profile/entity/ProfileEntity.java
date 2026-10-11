package com.starrainnotes.profile.entity;

import java.time.LocalDateTime;
import lombok.Data;

/*
 * 作者公开 Profile 实体。列名到属性名的映射由 application.yml 的
 * map-underscore-to-camel-case 完成，表名由 ProfileMapper.xml 的语句写明。
 * 本模块 Mapper 已不继承 BaseMapper，因此不保留 @TableName / @TableId / @TableField。
 *
 * headline / bioMarkdown / locationText / avatarMediaAssetId / resumeMediaAssetId 原先靠
 * @TableField(updateStrategy = FieldStrategy.ALWAYS) 保证「为 null 也写进 UPDATE」（清空文案、
 * 解除媒体引用）。这个语义现在由 ProfileMapper.xml 的 updateBasic / updateMediaAssets 显式承担。
 */
@Data
public class ProfileEntity {
    private Long id;
    private String profileKey;
    private String displayName;
    private String headline;
    private String bioMarkdown;
    private String locationText;
    private Long avatarMediaAssetId;
    private Long resumeMediaAssetId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
