/*
 * TutorialEntity：教程与学习模块的持久化实体文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */

package com.starrainnotes.tutorial.content.entity;

import java.time.LocalDateTime;
import lombok.Data;

/*
 * 教程工作区实体。列名到属性名的映射由 map-underscore-to-camel-case 完成，
 * 表名由 TutorialMapper.xml 的语句写明；本模块 Mapper 已不继承 BaseMapper，
 * 因此不保留 @TableName / @TableId / @TableField。
 *
 * withdrawn_at 原先靠 @TableField(updateStrategy = FieldStrategy.ALWAYS) 保证
 * 「为 null 也写进 UPDATE」，这个语义已由 TutorialMapper.xml 的 update 语句显式承担。
 */
/**
 * 教程实体，属于教程与学习模块。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */
@Data
public class TutorialEntity {
    /**
     * 当前业务对象的数据库标识。
     */
    private Long id;
    /**
     * 分类标识。
     */
    private Long categoryId;
    /**
     * 公开固定链接的路径标识。
     */
    private String slug;
    /**
     * 标题。
     */
    private String title;
    /**
     * 摘要。
     */
    private String summary;
    /**
     * 展示排序值。
     */
    private Integer sortOrder;
    /**
     * 封面媒体资产标识。
     */
    private Long coverMediaAssetId;
    /**
     * 发布状态。
     */
    private String publicationStatus;
    /**
     * editing状态。
     */
    private String editingStatus;
    /**
     * 已发布修订标识。
     */
    private Long publishedRevisionId;
    /**
     * 已发布时间。
     */
    private LocalDateTime publishedAt;
    /**
     * 已撤回时间。
     */
    private LocalDateTime withdrawnAt;
    /**
     * 创建该记录的账号标识。
     */
    private Long createdByAccountId;
    /**
     * 最近一次更新该记录的账号标识。
     */
    private Long updatedByAccountId;
    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
