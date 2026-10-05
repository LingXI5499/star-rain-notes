package com.starrainnotes.english.overview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sr_english_overview")
public class EnglishOverviewEntity {
    @TableId(type = IdType.INPUT)
    private Integer id;
    private String title;
    private String subtitle;
    private String introduction;
    private String currentStage;
    private String roadmapMarkdown;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
