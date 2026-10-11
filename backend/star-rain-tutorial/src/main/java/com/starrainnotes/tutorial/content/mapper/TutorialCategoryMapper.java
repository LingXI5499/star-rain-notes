/*
 * TutorialCategoryMapper：教程与学习模块的数据访问文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：TutorialCategoryEntity。
 * 阅读入口：insert（写入教程分类）；selectById（按标识读取查询）；listOrdered（列出Ordered）；countBySlug（统计依据固定链接标识）。
 */

package com.starrainnotes.tutorial.content.mapper;

import com.starrainnotes.tutorial.content.entity.TutorialCategoryEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/*
 * 知识体系分类的全部 SQL 都在 mapper/tutorial/TutorialCategoryMapper.xml。
 * 本接口不继承 BaseMapper，每个方法在 XML 里都有唯一对应的语句。
 */
/**
 * 教程分类数据映射，属于教程与学习模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
@Mapper
public interface TutorialCategoryMapper {

    /**
     * 写入教程分类。
     *
     * 持久化操作：新增，涉及 <code>sr_tutorial_category</code>。
     *
     * @param category 分类；类型为 <code>TutorialCategoryEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int insert(TutorialCategoryEntity category);

    /**
     * 按标识读取查询。
     *
     * 持久化操作：读取，涉及 <code>sr_tutorial_category</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>TutorialCategoryEntity</code> 的处理结果
     */
    TutorialCategoryEntity selectById(@Param("id") Long id);

    /**
     * 列出Ordered。
     *
     * 持久化操作：读取，涉及 <code>sr_tutorial_category</code>、<code>sr_tutorial</code>、<code>sr_tutorial_chapter</code>。
     * @return 符合 SQL 条件的记录数量
     */
    List<TutorialCategoryEntity> listOrdered();

    /**
     * 统计依据固定链接标识。
     *
     * 持久化操作：读取，涉及 <code>sr_tutorial_category</code>。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("slug")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long countBySlug(@Param("slug") String slug);

    /* 改名不动 slug：公开地址保持稳定。 */
    /**
     * 更新名称。
     *
     * 持久化操作：更新，涉及 <code>sr_tutorial_category</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param name 名称；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("name")</code>
     * @param updatedAt 更新时间；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("updatedAt")</code>
     * @return SQL 写入影响的记录行数
     */
    int updateName(@Param("id") Long id, @Param("name") String name,
                   @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * 更新排序顺序。
     *
     * 持久化操作：更新，涉及 <code>sr_tutorial_category</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param sortOrder 展示排序值；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("sortOrder")</code>
     * @param updatedAt 更新时间；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("updatedAt")</code>
     * @return SQL 写入影响的记录行数
     */
    int updateSortOrder(@Param("id") Long id, @Param("sortOrder") int sortOrder,
                        @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * 删除依据标识。
     *
     * 持久化操作：删除，涉及 <code>sr_tutorial_category</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return SQL 写入影响的记录行数
     */
    int deleteById(@Param("id") Long id);
}
