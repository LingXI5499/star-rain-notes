/*
 * TutorialMapper：教程与学习模块的数据访问文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：TutorialEntity、TutorialDashboardStatsDTO。
 * 阅读入口：insert（写入教程）；selectById（按标识读取查询）；byIdForUpdate（依据标识面向更新处理入口）；selectPublishedBySlug（按固定链接标识读取查询已发布）。
 */

package com.starrainnotes.tutorial.content.mapper;

import com.starrainnotes.tutorial.content.entity.TutorialEntity;
import com.starrainnotes.tutorial.content.dto.TutorialDashboardStatsDTO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/*
 * 教程工作区的全部 SQL 都在 mapper/tutorial/TutorialMapper.xml。
 *
 * 本接口不再继承 MyBatis-Plus 的 BaseMapper：selectById / insert / updateById 这些语句
 * 原先由框架在运行期拼接，源码里看不到实际 SQL。逐个访问形状显式声明后，
 * 每个方法都能在 XML 里找到唯一对应的一条语句（star-rain-boot 的 MapperXmlIntegrationTest 强制校验）。
 */
/**
 * 教程数据映射，属于教程与学习模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
@Mapper
public interface TutorialMapper {

    /**
     * 写入教程。
     *
     * 持久化操作：新增，涉及 <code>sr_tutorial</code>。
     *
     * @param tutorial 教程；类型为 <code>TutorialEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int insert(TutorialEntity tutorial);

    /**
     * 按标识读取查询。
     *
     * 持久化操作：读取，涉及 <code>sr_tutorial</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>TutorialEntity</code> 的处理结果
     */
    TutorialEntity selectById(@Param("id") Long id);

    /* 工作区写路径先锁行：发布、撤回、结构变更都要求同一行串行。 */
    /**
     * 依据标识面向更新处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_tutorial</code>。
     * 此查询使用行锁；调用方应在事务中完成后续状态检查和写入。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>TutorialEntity</code> 的处理结果
     */
    TutorialEntity byIdForUpdate(@Param("id") Long id);

    /* 公开读取只认 PUBLISHED，且必须已经冻结过版本。 */
    /**
     * 按固定链接标识读取查询已发布。
     *
     * 持久化操作：读取，涉及 <code>sr_tutorial</code>。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("slug")</code>
     * @return 类型为 <code>TutorialEntity</code> 的处理结果
     */
    TutorialEntity selectPublishedBySlug(@Param("slug") String slug);

    /* categoryId 为 null 表示「全部知识体系」，用于后台列表。 */
    /**
     * 列出Ordered。
     *
     * 持久化操作：读取，涉及 <code>sr_tutorial</code>。
     *
     * @param categoryId 分类标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("categoryId")</code>
     * @return 本次处理得到的条目集合
     */
    List<TutorialEntity> listOrdered(@Param("categoryId") Long categoryId);
    /**
     * 后台分页处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_tutorial</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param categoryId 分类标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("categoryId")</code>
     * @param keyword 关键词；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("keyword")</code>
     * @param offset 查询跳过的记录数；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("offset")</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("limit")</code>
     * @return 本次处理得到的条目集合
     */
    List<TutorialEntity> adminPage(@Param("categoryId") Long categoryId, @Param("keyword") String keyword,
            @Param("offset") long offset, @Param("limit") int limit);
    /**
     * 后台数量处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_tutorial</code>。
     *
     * @param categoryId 分类标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("categoryId")</code>
     * @param keyword 关键词；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("keyword")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long adminCount(@Param("categoryId") Long categoryId, @Param("keyword") String keyword);
    /**
     * 顺序条目集合处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_tutorial</code>。
     *
     * @param categoryId 分类标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("categoryId")</code>
     * @return 本次处理得到的条目集合
     */
    List<com.starrainnotes.tutorial.content.vo.TutorialOrderItemVO> orderItems(@Param("categoryId") Long categoryId);

    /* 公开目录：已发布且已有公开版本。 */
    /**
     * 列出已发布包含修订。
     *
     * 持久化操作：读取，涉及 <code>sr_tutorial</code>。
     * @return 本次处理得到的条目集合
     */
    List<TutorialEntity> listPublishedWithRevision();

    /* 学习侧按「已发布」扫描公开快照，不额外要求 published_revision_id 非空。 */
    /**
     * 列出已发布。
     *
     * 持久化操作：读取，涉及 <code>sr_tutorial</code>。
     * @return 本次处理得到的条目集合
     */
    List<TutorialEntity> listPublished();

    /**
     * 统计依据固定链接标识。
     *
     * 持久化操作：读取，涉及 <code>sr_tutorial</code>。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("slug")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long countBySlug(@Param("slug") String slug);

    /**
     * 统计依据分类标识。
     *
     * 持久化操作：读取，涉及 <code>sr_tutorial</code>。
     *
     * @param categoryId 分类标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("categoryId")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long countByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * 概览统计字段处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_tutorial</code>、<code>sr_tutorial_chapter</code>。
     * @return 符合 SQL 条件的记录数量
     */
    TutorialDashboardStatsDTO dashboardStats();

    /**
     * 概览Recent处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_tutorial</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     * @return 本次处理得到的条目集合
     */
    List<TutorialEntity> dashboardRecent();

    /* 整行保存工作区改动；withdrawn_at 必须写进 SET，null 也要覆盖（旧 @TableField(ALWAYS) 语义）。 */
    /**
     * 更新教程。
     *
     * 持久化操作：更新，涉及 <code>sr_tutorial</code>。
     *
     * @param tutorial 教程；类型为 <code>TutorialEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int update(TutorialEntity tutorial);

    /**
     * 更新排序顺序。
     *
     * 持久化操作：更新，涉及 <code>sr_tutorial</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param sortOrder 展示排序值；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("sortOrder")</code>
     * @return SQL 写入影响的记录行数
     */
    int updateSortOrder(@Param("id") Long id, @Param("sortOrder") int sortOrder);

    /**
     * 删除依据标识。
     *
     * 持久化操作：删除，涉及 <code>sr_tutorial</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return SQL 写入影响的记录行数
     */
    int deleteById(@Param("id") Long id);
}
