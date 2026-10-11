/*
 * MessageEntity：留言模块的持久化实体文件。
 * 业务范围：留言提交、审核展示和后台管理。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */

package com.starrainnotes.message.entity;

import java.time.LocalDateTime;
import lombok.Data;

/*
 * 留言实体。列名到属性名的映射由 application.yml 的 map-underscore-to-camel-case 完成，
 * 表名由各 Mapper XML 的语句写明，因此这里不保留 MyBatis-Plus 的 @TableName / @TableId
 * （本模块的 Mapper 已不继承 BaseMapper，那两个注解不会再生效，留着只会让人误以为框架仍在拼 SQL）。
 */
/**
 * 留言实体，属于留言模块。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */
@Data
public class MessageEntity {
    /**
     * 当前业务对象的数据库标识。
     */
    private Long id;
    /**
     * 账号标识。
     */
    private Long accountId;
    /**
     * 作者显示名称。
     */
    private String authorDisplayName;
    /**
     * contact邮箱。
     */
    private String contactEmail;
    /**
     * 内容。
     */
    private String content;
    /**
     * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束。
     */
    private String status;
    /**
     * 提交时间。
     */
    private LocalDateTime submittedAt;
    /**
     * moderated依据账号标识。
     */
    private Long moderatedByAccountId;
    /**
     * moderated时间。
     */
    private LocalDateTime moderatedAt;
    /**
     * reject原因。
     */
    private String rejectReason;
    /**
     * hidden时间。
     */
    private LocalDateTime hiddenAt;
    /**
     * 已删除时间。
     */
    private LocalDateTime deletedAt;
    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
