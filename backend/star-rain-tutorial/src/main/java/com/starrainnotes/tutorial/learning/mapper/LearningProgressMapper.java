/*
 * LearningProgressMapper：教程与学习模块的数据访问文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：LearningProgressEntity。
 * 阅读入口：upsertReading（新增或更新阅读）；ensureRow（确保行）；markCompleted（mark已完成处理入口）；selectByAccountIdAndChapterId（按账号标识及章节标识读取查询）。
 */

package com.starrainnotes.tutorial.learning.mapper;

import com.starrainnotes.tutorial.learning.entity.LearningProgressEntity;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/*
 * 章节学习进度的全部 SQL 都在 mapper/tutorial/LearningProgressMapper.xml。
 * 本接口不继承 BaseMapper，每个方法在 XML 里都有唯一对应的语句。
 */
/**
 * 学习进度数据映射，属于教程与学习模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
@Mapper
public interface LearningProgressMapper {

    /* 同一账户同一章节只有一行：读到哪写到哪，学习时长累加。 */
    /**
     * 新增或更新阅读。
     *
     * 持久化操作：新增，涉及 <code>sr_learning_progress</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param tutorialId 教程标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("tutorialId")</code>
     * @param groupId 分组标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("groupId")</code>
     * @param chapterId 章节标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("chapterId")</code>
     * @param anchor anchor；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("anchor")</code>
     * @param ratio 比例；类型为 <code>BigDecimal</code>；对应 MyBatis 命名参数 <code>@Param("ratio")</code>
     * @param seconds 秒；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("seconds")</code>
     * @return SQL 写入影响的记录行数
     */
    int upsertReading(@Param("accountId") Long accountId,
                      @Param("tutorialId") Long tutorialId,
                      @Param("groupId") Long groupId,
                      @Param("chapterId") Long chapterId,
                      @Param("anchor") String anchor,
                      @Param("ratio") BigDecimal ratio,
                      @Param("seconds") int seconds);

    /**
     * 确保行。
     *
     * 持久化操作：新增，涉及 <code>sr_learning_progress</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param tutorialId 教程标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("tutorialId")</code>
     * @param groupId 分组标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("groupId")</code>
     * @param chapterId 章节标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("chapterId")</code>
     * @return SQL 写入影响的记录行数
     */
    int ensureRow(@Param("accountId") Long accountId,
                  @Param("tutorialId") Long tutorialId,
                  @Param("groupId") Long groupId,
                  @Param("chapterId") Long chapterId);

    /* 只在第一次完成时返回 1，用于「完成事件只写一次」的幂等判断。 */
    /**
     * mark已完成处理入口。
     *
     * 持久化操作：更新，涉及 <code>sr_learning_progress</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param chapterId 章节标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("chapterId")</code>
     * @return SQL 写入影响的记录行数
     */
    int markCompleted(@Param("accountId") Long accountId,
                      @Param("chapterId") Long chapterId);

    /**
     * 按账号标识及章节标识读取查询。
     *
     * 持久化操作：读取，涉及 <code>sr_learning_progress</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param chapterId 章节标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("chapterId")</code>
     * @return 类型为 <code>LearningProgressEntity</code> 的处理结果
     */
    LearningProgressEntity selectByAccountIdAndChapterId(@Param("accountId") Long accountId,
                                                        @Param("chapterId") Long chapterId);

    /**
     * 列出依据账号标识。
     *
     * 持久化操作：读取，涉及 <code>sr_learning_progress</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @return 本次处理得到的条目集合
     */
    List<LearningProgressEntity> listByAccountId(@Param("accountId") Long accountId);

    /**
     * 列出依据账号标识及教程标识。
     *
     * 持久化操作：读取，涉及 <code>sr_learning_progress</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param tutorialId 教程标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("tutorialId")</code>
     * @return 本次处理得到的条目集合
     */
    List<LearningProgressEntity> listByAccountIdAndTutorialId(@Param("accountId") Long accountId,
                                                              @Param("tutorialId") Long tutorialId);

    /* 继续阅读入口：按最近学习时间倒序取固定条数。 */
    /**
     * 列出Recent依据账号标识。
     *
     * 持久化操作：读取，涉及 <code>sr_learning_progress</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @return 本次处理得到的条目集合
     */
    List<LearningProgressEntity> listRecentByAccountId(@Param("accountId") Long accountId);
}
