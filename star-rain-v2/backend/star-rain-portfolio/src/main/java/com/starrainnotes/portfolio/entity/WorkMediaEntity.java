package com.starrainnotes.portfolio.entity;

import java.time.LocalDateTime;
import lombok.Data;

/*
 * 作品媒体编排实体。@TableName / @TableId 已移除，映射由 WorkMediaMapper.xml 承担。
 */
@Data
public class WorkMediaEntity {
    private Long id;
    private Long workId;
    private Long mediaAssetId;
    private String usageType;
    private String caption;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
