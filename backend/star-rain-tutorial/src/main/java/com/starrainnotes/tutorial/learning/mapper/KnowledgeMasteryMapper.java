/*
 * KnowledgeMasteryMapper：教程与学习模块的数据访问文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：KnowledgeMasteryEntity。
 * 阅读入口：ensure（确保知识掌握）；ensureLearned（确保已学习）；markLearned（mark已学习处理入口）；selectByAccountIdAndCardId（按账号标识及卡片标识读取查询）。
 */

package com.starrainnotes.tutorial.learning.mapper;

import com.starrainnotes.tutorial.learning.entity.KnowledgeMasteryEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/*
 * 知识掌握状态的全部 SQL 都在 mapper/tutorial/KnowledgeMasteryMapper.xml。
 * 本接口不继承 BaseMapper，每个方法在 XML 里都有唯一对应的语句。
 */
/**
 * 知识掌握数据映射，属于教程与学习模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
@Mapper
public interface KnowledgeMasteryMapper {

    /**
     * 确保知识掌握。
     *
     * 持久化操作：新增，涉及 <code>sr_knowledge_mastery</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param cardId 卡片标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("cardId")</code>
     * @return SQL 写入影响的记录行数
     */
    int ensure(@Param("accountId") Long accountId, @Param("cardId") Long cardId);

    /* 首次学习时间只在第一次写入，重复调用不会覆盖。 */
    /**
     * 确保已学习。
     *
     * 持久化操作：新增，涉及 <code>sr_knowledge_mastery</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param cardId 卡片标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("cardId")</code>
     * @return SQL 写入影响的记录行数
     */
    int ensureLearned(@Param("accountId") Long accountId, @Param("cardId") Long cardId);

    /**
     * mark已学习处理入口。
     *
     * 持久化操作：更新，涉及 <code>sr_knowledge_mastery</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param cardId 卡片标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("cardId")</code>
     * @return SQL 写入影响的记录行数
     */
    int markLearned(@Param("accountId") Long accountId, @Param("cardId") Long cardId);

    /**
     * 按账号标识及卡片标识读取查询。
     *
     * 持久化操作：读取，涉及 <code>sr_knowledge_mastery</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param knowledgeCardId 知识卡片标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("knowledgeCardId")</code>
     * @return 类型为 <code>KnowledgeMasteryEntity</code> 的处理结果
     */
    KnowledgeMasteryEntity selectByAccountIdAndCardId(@Param("accountId") Long accountId,
                                                      @Param("knowledgeCardId") Long knowledgeCardId);

    /**
     * 列出依据账号标识。
     *
     * 持久化操作：读取，涉及 <code>sr_knowledge_mastery</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @return 本次处理得到的条目集合
     */
    List<KnowledgeMasteryEntity> listByAccountId(@Param("accountId") Long accountId);

    /* 复习完成后回写证据统计与系统建议等级。 */
    /**
     * 更新证据。
     *
     * 持久化操作：更新，涉及 <code>sr_knowledge_mastery</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param evidenceCount 证据数量；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("evidenceCount")</code>
     * @param lastRecallRating 最近Recall评分；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("lastRecallRating")</code>
     * @param lastEvidenceAt 最近证据时间；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("lastEvidenceAt")</code>
     * @param systemSuggestedLevel systemSuggestedLevel；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("systemSuggestedLevel")</code>
     * @return SQL 写入影响的记录行数
     */
    int updateEvidence(@Param("id") Long id, @Param("evidenceCount") int evidenceCount,
                       @Param("lastRecallRating") String lastRecallRating,
                       @Param("lastEvidenceAt") LocalDateTime lastEvidenceAt,
                       @Param("systemSuggestedLevel") String systemSuggestedLevel);

    /**
     * 更新SelfLevel。
     *
     * 持久化操作：更新，涉及 <code>sr_knowledge_mastery</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param userSelfLevel 用户SelfLevel；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("userSelfLevel")</code>
     * @return SQL 写入影响的记录行数
     */
    int updateSelfLevel(@Param("id") Long id, @Param("userSelfLevel") String userSelfLevel);
}
