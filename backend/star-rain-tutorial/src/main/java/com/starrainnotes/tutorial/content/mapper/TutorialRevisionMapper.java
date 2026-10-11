/*
 * TutorialRevisionMapper：教程与学习模块的数据访问文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：TutorialRevisionEntity。
 * 阅读入口：insert（写入教程修订）；selectById（按标识读取查询）；selectByTutorialIdAndRef（按教程标识及Ref读取查询）；selectLatestByTutorialId（按教程标识读取查询最新）。
 */

package com.starrainnotes.tutorial.content.mapper;

import com.starrainnotes.tutorial.content.entity.TutorialRevisionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/*
 * 教程不可变版本快照的全部 SQL 都在 mapper/tutorial/TutorialRevisionMapper.xml。
 * 本接口不继承 BaseMapper，每个方法在 XML 里都有唯一对应的语句。
 */
/**
 * 教程修订数据映射，属于教程与学习模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
@Mapper
public interface TutorialRevisionMapper {

    /**
     * 写入教程修订。
     *
     * 持久化操作：新增，涉及 <code>sr_tutorial_revision</code>。
     *
     * @param revision 修订；类型为 <code>TutorialRevisionEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int insert(TutorialRevisionEntity revision);

    /**
     * 按标识读取查询。
     *
     * 持久化操作：读取，涉及 <code>sr_tutorial_revision</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>TutorialRevisionEntity</code> 的处理结果
     */
    TutorialRevisionEntity selectById(@Param("id") Long id);

    /* 审核按 revision_ref 精确定位版本。 */
    /**
     * 按教程标识及Ref读取查询。
     *
     * 持久化操作：读取，涉及 <code>sr_tutorial_revision</code>。
     *
     * @param tutorialId 教程标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("tutorialId")</code>
     * @param revisionRef 版本或修订的引用值；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("revisionRef")</code>
     * @return 类型为 <code>TutorialRevisionEntity</code> 的处理结果
     */
    TutorialRevisionEntity selectByTutorialIdAndRef(@Param("tutorialId") Long tutorialId,
                                                    @Param("revisionRef") String revisionRef);

    /* 审核通过前必须确认这是最新版本，避免旧版本覆盖新版本。 */
    /**
     * 按教程标识读取查询最新。
     *
     * 持久化操作：读取，涉及 <code>sr_tutorial_revision</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param tutorialId 教程标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("tutorialId")</code>
     * @return 类型为 <code>TutorialRevisionEntity</code> 的处理结果
     */
    TutorialRevisionEntity selectLatestByTutorialId(@Param("tutorialId") Long tutorialId);

    /**
     * 统计依据教程标识。
     *
     * 持久化操作：读取，涉及 <code>sr_tutorial_revision</code>。
     *
     * @param tutorialId 教程标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("tutorialId")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long countByTutorialId(@Param("tutorialId") Long tutorialId);
}
