/*
 * MessageActionEntity：留言模块的持久化实体文件。
 * 业务范围：留言提交、审核展示和后台管理。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */

package com.starrainnotes.message.entity;

import java.time.LocalDateTime;
import lombok.Data;

/* 留言操作流水实体；表名与列名由 MessageActionMapper.xml 的语句写明，无需框架注解。 */
/**
 * 留言动作实体，属于留言模块。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */
@Data
public class MessageActionEntity {
    /**
     * 当前业务对象的数据库标识。
     */
    private Long id;
    /**
     * 留言标识。
     */
    private Long messageId;
    /**
     * 动作类型。
     */
    private String actionType;
    /**
     * 操作者账号标识。
     */
    private Long actorAccountId;
    /**
     * 操作者类型。
     */
    private String actorType;
    /**
     * note。
     */
    private String note;
    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
}
