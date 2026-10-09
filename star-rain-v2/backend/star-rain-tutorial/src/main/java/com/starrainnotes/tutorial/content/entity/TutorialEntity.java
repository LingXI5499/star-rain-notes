package com.starrainnotes.tutorial.content.entity;

import java.time.LocalDateTime;
import lombok.Data;

/*
 * 教程工作区实体。列名到属性名的映射由 map-underscore-to-camel-case 完成，
 * 表名由 TutorialMapper.xml 的语句写明；本模块 Mapper 已不继承 BaseMapper，
 * 因此不保留 @TableName / @TableId / @TableField。
 *
 * withdrawn_at 原先靠 @TableField(updateStrategy = FieldStrategy.ALWAYS) 保证
 * 「为 null 也写进 UPDATE」，这个语义已由 TutorialMapper.xml 的 update 语句显式承担。
 */
@Data
public class TutorialEntity {
    private Long id;
    private Long categoryId;
    private String slug;
    private String title;
    private String summary;
    private Integer sortOrder;
    private Long coverMediaAssetId;
    private String publicationStatus;
    private String editingStatus;
    private Long publishedRevisionId;
    private LocalDateTime publishedAt;
    private LocalDateTime withdrawnAt;
    private Long createdByAccountId;
    private Long updatedByAccountId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
