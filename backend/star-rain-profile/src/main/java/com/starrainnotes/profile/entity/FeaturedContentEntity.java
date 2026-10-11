package com.starrainnotes.profile.entity;

import java.time.LocalDateTime;
import lombok.Data;

/*
 * 作者精选内容引用实体。表名与列映射由 FeaturedContentMapper.xml 承担。
 *
 * titleOverride 原先带 @TableField(updateStrategy = FieldStrategy.ALWAYS)，但这条路径上
 * 没有「编辑精选」的业务，只有新增与排序；新增时 null 写入 NULL 与列默认值一致。
 */
@Data
public class FeaturedContentEntity {
    private Long id;
    private Long profileId;
    private String contentType;
    private Long contentId;
    private String titleOverride;
    private Integer sortOrder;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
