/*
 * TutorialKnowledgeCardMapper：教程与学习模块的数据访问文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：TutorialKnowledgeCardEntity。
 * 阅读入口：saveWorkspace（保存Workspace）；insert（写入教程知识卡片）；selectById（按标识读取查询）；listByChapterId（列出依据章节标识）。
 */

package com.starrainnotes.tutorial.content.mapper;

import com.starrainnotes.tutorial.content.entity.TutorialKnowledgeCardEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/*
 * 章节知识卡片的全部 SQL 都在 mapper/tutorial/TutorialKnowledgeCardMapper.xml。
 * 本接口不继承 BaseMapper，每个方法在 XML 里都有唯一对应的语句。
 */
/**
 * 教程知识卡片数据映射，属于教程与学习模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
@Mapper
public interface TutorialKnowledgeCardMapper {

    /**
     * 保存Workspace。
     *
     * 持久化操作：更新，涉及 <code>sr_tutorial_knowledge_card</code>。
     *
     * @param row 行；类型为 <code>TutorialKnowledgeCardEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int saveWorkspace(TutorialKnowledgeCardEntity row);

    /**
     * 写入教程知识卡片。
     *
     * 持久化操作：新增，涉及 <code>sr_tutorial_knowledge_card</code>。
     *
     * @param card 卡片；类型为 <code>TutorialKnowledgeCardEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int insert(TutorialKnowledgeCardEntity card);

    /**
     * 按标识读取查询。
     *
     * 持久化操作：读取，涉及 <code>sr_tutorial_knowledge_card</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>TutorialKnowledgeCardEntity</code> 的处理结果
     */
    TutorialKnowledgeCardEntity selectById(@Param("id") Long id);

    /**
     * 列出依据章节标识。
     *
     * 持久化操作：读取，涉及 <code>sr_tutorial_knowledge_card</code>。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("chapterId")</code>
     * @return 本次处理得到的条目集合
     */
    List<TutorialKnowledgeCardEntity> listByChapterId(@Param("chapterId") Long chapterId);

    /* 发布冻结快照只取 ENABLED 卡片。 */
    /**
     * 列出已启用依据章节标识。
     *
     * 持久化操作：读取，涉及 <code>sr_tutorial_knowledge_card</code>。
     *
     * @param chapterId 章节标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("chapterId")</code>
     * @return 本次处理得到的条目集合
     */
    List<TutorialKnowledgeCardEntity> listEnabledByChapterId(@Param("chapterId") Long chapterId);

    /**
     * 更新内容。
     *
     * 持久化操作：更新，涉及 <code>sr_tutorial_knowledge_card</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param frontText 知识卡片正面的提示文本；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("frontText")</code>
     * @param backMarkdown 知识卡片背面的 Markdown 内容；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("backMarkdown")</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("status")</code>
     * @param updatedAt 更新时间；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("updatedAt")</code>
     * @return SQL 写入影响的记录行数
     */
    int updateContent(@Param("id") Long id, @Param("frontText") String frontText,
                      @Param("backMarkdown") String backMarkdown, @Param("status") String status,
                      @Param("updatedAt") LocalDateTime updatedAt);

    /* 排序只改 sort_order，与原先 updateById 的字段范围一致（不碰 updated_at）。 */
    /**
     * 更新排序顺序。
     *
     * 持久化操作：更新，涉及 <code>sr_tutorial_knowledge_card</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param sortOrder 展示排序值；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("sortOrder")</code>
     * @return SQL 写入影响的记录行数
     */
    int updateSortOrder(@Param("id") Long id, @Param("sortOrder") int sortOrder);

    /**
     * 删除依据标识。
     *
     * 持久化操作：删除，涉及 <code>sr_tutorial_knowledge_card</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return SQL 写入影响的记录行数
     */
    int deleteById(@Param("id") Long id);
}
