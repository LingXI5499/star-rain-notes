package com.starrainnotes.profile.entity;

import java.time.LocalDateTime;
import lombok.Data;

/*
 * 作者外部社交链接实体。@TableName / @TableId 已移除，映射由 SocialLinkMapper.xml 承担。
 */
@Data
public class SocialLinkEntity {
    private Long id;
    private Long profileId;
    private String platformCode;
    private String label;
    private String url;
    private Integer sortOrder;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
