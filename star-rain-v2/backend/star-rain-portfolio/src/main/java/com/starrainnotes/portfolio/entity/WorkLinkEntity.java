package com.starrainnotes.portfolio.entity;

import java.time.LocalDateTime;
import lombok.Data;

/*
 * 作品外部链接实体。@TableName / @TableId 已移除，映射由 WorkLinkMapper.xml 承担。
 */
@Data
public class WorkLinkEntity {
    private Long id;
    private Long workId;
    private String linkType;
    private String label;
    private String url;
    private Integer sortOrder;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
