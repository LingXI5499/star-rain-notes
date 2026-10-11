/*
 * TutorialChapterMapper：教程与学习模块的数据访问文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：TutorialChapterEntity。
 * 阅读入口：saveWorkspace（保存Workspace）；bumpVersion（bump版本处理入口）；insert（写入教程章节）；selectById（按标识读取查询）。
 */

package com.starrainnotes.tutorial.content.mapper;

import com.starrainnotes.tutorial.content.entity.TutorialChapterEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/*
 * 章节工作区的全部 SQL 都在 mapper/tutorial/TutorialChapterMapper.xml。
 * 本接口不继承 BaseMapper，每个方法在 XML 里都有唯一对应的语句。
 */
/**
 * 教程章节数据映射，属于教程与学习模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
@Mapper
public interface TutorialChapterMapper {

    /**
     * 保存Workspace。
     *
     * 持久化操作：更新，涉及 <code>sr_tutorial_chapter</code>。
     *
     * @param chapter 章节；类型为 <code>TutorialChapterEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int saveWorkspace(TutorialChapterEntity chapter);

    /**
     * bump版本处理入口。
     *
     * 持久化操作：更新，涉及 <code>sr_tutorial_chapter</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return SQL 写入影响的记录行数
     */
    int bumpVersion(@Param("id") Long id);

    /**
     * 写入教程章节。
     *
     * 持久化操作：新增，涉及 <code>sr_tutorial_chapter</code>。
     *
     * @param chapter 章节；类型为 <code>TutorialChapterEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int insert(TutorialChapterEntity chapter);

    /**
     * 按标识读取查询。
     *
     * 持久化操作：读取，涉及 <code>sr_tutorial_chapter</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>TutorialChapterEntity</code> 的处理结果
     */
    TutorialChapterEntity selectById(@Param("id") Long id);

    /**
     * 列出依据分组标识。
     *
     * 持久化操作：读取，涉及 <code>sr_tutorial_chapter</code>。
     *
     * @param groupId 分组标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("groupId")</code>
     * @return 本次处理得到的条目集合
     */
    List<TutorialChapterEntity> listByGroupId(@Param("groupId") Long groupId);

    /* 发布冻结快照只取 ACTIVE 章节。 */
    /**
     * 列出有效依据分组标识。
     *
     * 持久化操作：读取，涉及 <code>sr_tutorial_chapter</code>。
     *
     * @param groupId 分组标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("groupId")</code>
     * @return 本次处理得到的条目集合
     */
    List<TutorialChapterEntity> listActiveByGroupId(@Param("groupId") Long groupId);

    /**
     * 统计依据教程标识。
     *
     * 持久化操作：读取，涉及 <code>sr_tutorial_chapter</code>。
     *
     * @param tutorialId 教程标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("tutorialId")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long countByTutorialId(@Param("tutorialId") Long tutorialId);

    /**
     * 统计有效依据分组标识。
     *
     * 持久化操作：读取，涉及 <code>sr_tutorial_chapter</code>。
     *
     * @param groupId 分组标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("groupId")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long countActiveByGroupId(@Param("groupId") Long groupId);

    /**
     * 统计依据教程标识及固定链接标识。
     *
     * 持久化操作：读取，涉及 <code>sr_tutorial_chapter</code>。
     *
     * @param tutorialId 教程标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("tutorialId")</code>
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("slug")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long countByTutorialIdAndSlug(@Param("tutorialId") Long tutorialId, @Param("slug") String slug);

    /* summary 允许被清空：必须无条件写进 SET（旧 @TableField(ALWAYS) 语义）。 */
    /**
     * 更新内容。
     *
     * 持久化操作：更新，涉及 <code>sr_tutorial_chapter</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param title 标题；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("title")</code>
     * @param summary 摘要；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("summary")</code>
     * @param updatedAt 更新时间；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("updatedAt")</code>
     * @return SQL 写入影响的记录行数
     */
    int updateContent(@Param("id") Long id, @Param("title") String title,
                      @Param("summary") String summary, @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * 更新正文。
     *
     * 持久化操作：更新，涉及 <code>sr_tutorial_chapter</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param bodyMarkdown Markdown 格式的正文；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("bodyMarkdown")</code>
     * @param updatedAt 更新时间；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("updatedAt")</code>
     * @return SQL 写入影响的记录行数
     */
    int updateBody(@Param("id") Long id, @Param("bodyMarkdown") String bodyMarkdown,
                   @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * 更新状态。
     *
     * 持久化操作：更新，涉及 <code>sr_tutorial_chapter</code>。
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
     * 持久化操作：更新，涉及 <code>sr_tutorial_chapter</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param sortOrder 展示排序值；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("sortOrder")</code>
     * @param updatedAt 更新时间；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("updatedAt")</code>
     * @return SQL 写入影响的记录行数
     */
    int updateSortOrder(@Param("id") Long id, @Param("sortOrder") int sortOrder,
                        @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * move目标分组处理入口。
     *
     * 持久化操作：更新，涉及 <code>sr_tutorial_chapter</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param groupId 分组标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("groupId")</code>
     * @param sortOrder 展示排序值；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("sortOrder")</code>
     * @param updatedAt 更新时间；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("updatedAt")</code>
     * @return SQL 写入影响的记录行数
     */
    int moveToGroup(@Param("id") Long id, @Param("groupId") Long groupId,
                    @Param("sortOrder") int sortOrder, @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * 删除依据标识。
     *
     * 持久化操作：删除，涉及 <code>sr_tutorial_chapter</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return SQL 写入影响的记录行数
     */
    int deleteById(@Param("id") Long id);
}
