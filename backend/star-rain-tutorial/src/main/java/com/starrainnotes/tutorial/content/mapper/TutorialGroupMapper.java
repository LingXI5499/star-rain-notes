/*
 * TutorialGroupMapper：教程与学习模块的数据访问文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：TutorialGroupEntity。
 * 阅读入口：insert（写入教程分组）；selectById（按标识读取查询）；listByTutorialId（列出依据教程标识）；listActiveByTutorialId（列出有效依据教程标识）。
 */

package com.starrainnotes.tutorial.content.mapper;

import com.starrainnotes.tutorial.content.entity.TutorialGroupEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/*
 * 教程分组的全部 SQL 都在 mapper/tutorial/TutorialGroupMapper.xml。
 * 本接口不继承 BaseMapper，每个方法在 XML 里都有唯一对应的语句。
 */
/**
 * 教程分组数据映射，属于教程与学习模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
@Mapper
public interface TutorialGroupMapper {

    /**
     * 写入教程分组。
     *
     * 持久化操作：新增，涉及 <code>sr_tutorial_group</code>。
     *
     * @param group 分组；类型为 <code>TutorialGroupEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int insert(TutorialGroupEntity group);

    /**
     * 按标识读取查询。
     *
     * 持久化操作：读取，涉及 <code>sr_tutorial_group</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>TutorialGroupEntity</code> 的处理结果
     */
    TutorialGroupEntity selectById(@Param("id") Long id);

    /**
     * 列出依据教程标识。
     *
     * 持久化操作：读取，涉及 <code>sr_tutorial_group</code>。
     *
     * @param tutorialId 教程标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("tutorialId")</code>
     * @return 本次处理得到的条目集合
     */
    List<TutorialGroupEntity> listByTutorialId(@Param("tutorialId") Long tutorialId);

    /* 发布冻结快照只取 ACTIVE 分组。 */
    /**
     * 列出有效依据教程标识。
     *
     * 持久化操作：读取，涉及 <code>sr_tutorial_group</code>。
     *
     * @param tutorialId 教程标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("tutorialId")</code>
     * @return 本次处理得到的条目集合
     */
    List<TutorialGroupEntity> listActiveByTutorialId(@Param("tutorialId") Long tutorialId);

    /**
     * 统计依据教程标识。
     *
     * 持久化操作：读取，涉及 <code>sr_tutorial_group</code>。
     *
     * @param tutorialId 教程标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("tutorialId")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long countByTutorialId(@Param("tutorialId") Long tutorialId);

    /**
     * 更新标题。
     *
     * 持久化操作：更新，涉及 <code>sr_tutorial_group</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param title 标题；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("title")</code>
     * @param updatedAt 更新时间；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("updatedAt")</code>
     * @return SQL 写入影响的记录行数
     */
    int updateTitle(@Param("id") Long id, @Param("title") String title,
                    @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * 更新状态。
     *
     * 持久化操作：更新，涉及 <code>sr_tutorial_group</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("status")</code>
     * @param updatedAt 更新时间；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("updatedAt")</code>
     * @return SQL 写入影响的记录行数
     */
    int updateStatus(@Param("id") Long id, @Param("status") String status,
                     @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * 更新排序顺序。
     *
     * 持久化操作：更新，涉及 <code>sr_tutorial_group</code>。
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
     * 持久化操作：删除，涉及 <code>sr_tutorial_group</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return SQL 写入影响的记录行数
     */
    int deleteById(@Param("id") Long id);
}
