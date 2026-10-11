/*
 * WorkDetailEntity：作品模块的持久化实体文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */

package com.starrainnotes.portfolio.entity;

import java.time.LocalDateTime;
import lombok.Data;

/*
 * 作品类型扩展信息实体。@TableName / @TableId 已移除，映射由 WorkDetailMapper.xml 承担。
 */
/**
 * 作品详情实体，属于作品模块。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */
@Data
public class WorkDetailEntity {
    /**
     * 当前业务对象的数据库标识。
     */
    private Long id;
    /**
     * 作品标识。
     */
    private Long workId;
    /**
     * 作品类型。
     */
    private String workType;
    /**
     * 详情JSON。
     */
    private String detailJson;
    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
