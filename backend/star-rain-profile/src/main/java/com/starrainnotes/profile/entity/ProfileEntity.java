/*
 * ProfileEntity：作者资料模块的持久化实体文件。
 * 业务范围：个人介绍、社交链接、经历技能和精选内容。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */

package com.starrainnotes.profile.entity;

import java.time.LocalDateTime;
import lombok.Data;

/*
 * 作者公开 Profile 实体。列名到属性名的映射由 application.yml 的
 * map-underscore-to-camel-case 完成，表名由 ProfileMapper.xml 的语句写明。
 * 本模块 Mapper 已不继承 BaseMapper，因此不保留 @TableName / @TableId / @TableField。
 *
 * headline / bioMarkdown / locationText / avatarMediaAssetId / resumeMediaAssetId 原先靠
 * @TableField(updateStrategy = FieldStrategy.ALWAYS) 保证「为 null 也写进 UPDATE」（清空文案、
 * 解除媒体引用）。这个语义现在由 ProfileMapper.xml 的 updateBasic / updateMediaAssets 显式承担。
 */
/**
 * 资料实体，属于作者资料模块。
 * 承接数据库查询结果和写入数据，字段名称与映射 SQL 配合使用；它不直接决定接口的公开展示范围。
 */
@Data
public class ProfileEntity {
    /**
     * 当前业务对象的数据库标识。
     */
    private Long id;
    /**
     * 资料键。
     */
    private String profileKey;
    /**
     * 显示名称。
     */
    private String displayName;
    /**
     * headline。
     */
    private String headline;
    /**
     * bioMarkdown。
     */
    private String bioMarkdown;
    /**
     * location文本。
     */
    private String locationText;
    /**
     * avatar媒体资产标识。
     */
    private Long avatarMediaAssetId;
    /**
     * resume媒体资产标识。
     */
    private Long resumeMediaAssetId;
    /**
     * 当前业务状态，合法取值由本模块状态定义和状态转换规则约束。
     */
    private String status;
    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
