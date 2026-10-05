package com.starrainnotes.portfolio.entity;

import java.time.LocalDateTime;
import lombok.Data;

/*
 * 作品类型扩展信息实体。@TableName / @TableId 已移除，映射由 WorkDetailMapper.xml 承担。
 */
@Data
public class WorkDetailEntity {
    private Long id;
    private Long workId;
    private String workType;
    private String detailJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
