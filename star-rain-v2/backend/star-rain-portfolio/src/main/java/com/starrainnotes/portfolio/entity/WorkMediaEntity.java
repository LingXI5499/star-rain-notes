package com.starrainnotes.portfolio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sr_portfolio_media")
public class WorkMediaEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long workId;
    private Long mediaAssetId;
    private String usageType;
    private String caption;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
