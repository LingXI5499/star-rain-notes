package com.starrainnotes.portfolio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sr_portfolio_link")
public class WorkLinkEntity {
    @TableId(type = IdType.AUTO)
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
