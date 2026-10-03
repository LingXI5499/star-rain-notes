package com.starrainnotes.tutorial.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sr_tutorial_revision")
public class TutorialRevisionEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tutorialId;
    private Integer revisionNo;
    private String revisionRef;
    private String snapshotJson;
    private String contentSha256;
    private String revisionReason;
    private Long createdByAccountId;
    private LocalDateTime createdAt;
}
