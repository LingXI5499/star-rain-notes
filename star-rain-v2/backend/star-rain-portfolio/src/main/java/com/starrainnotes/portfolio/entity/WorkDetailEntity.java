package com.starrainnotes.portfolio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sr_portfolio_work_detail")
public class WorkDetailEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long workId;
    private String workType;
    private String detailJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
