package com.starrainnotes.portfolio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sr_portfolio_work")
public class WorkEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String slug;
    private String workType;
    private String title;
    private String summary;
    private String bodyMarkdown;
    private String status;
    private LocalDateTime publishedAt;
    private LocalDateTime withdrawnAt;
    private Long createdByAccountId;
    private Long updatedByAccountId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
