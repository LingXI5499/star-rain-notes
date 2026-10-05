package com.starrainnotes.tutorial.content.entity;

import java.time.LocalDateTime;
import lombok.Data;

/*
 * 教程不可变版本快照实体。表名与列清单由 mapper/tutorial/TutorialRevisionMapper.xml 维护，
 * 不再使用 MyBatis-Plus 的表注解。
 */
@Data
public class TutorialRevisionEntity {
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
