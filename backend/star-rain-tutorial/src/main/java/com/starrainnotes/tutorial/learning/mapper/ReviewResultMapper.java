/*
 * ReviewResultMapper：教程与学习模块的数据访问文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：ReviewResultEntity。
 * 阅读入口：insert（写入审核结果）；countGoodByAccountIdAndCardId（统计Good依据账号标识及卡片标识）。
 */

package com.starrainnotes.tutorial.learning.mapper;

import com.starrainnotes.tutorial.learning.entity.ReviewResultEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/*
 * 单次复习结果的全部 SQL 都在 mapper/tutorial/ReviewResultMapper.xml。
 * 本接口不继承 BaseMapper，每个方法在 XML 里都有唯一对应的语句。
 */
/**
 * 审核结果数据映射，属于教程与学习模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
@Mapper
public interface ReviewResultMapper {

    /**
     * 写入审核结果。
     *
     * 持久化操作：新增，涉及 <code>sr_review_result</code>。
     *
     * @param result 结果；类型为 <code>ReviewResultEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int insert(ReviewResultEntity result);

    /* 系统建议等级依据：这张卡片历史上「正常/轻松」的回忆次数。 */
    /**
     * 统计Good依据账号标识及卡片标识。
     *
     * 持久化操作：读取，涉及 <code>sr_review_result</code>。
     *
     * @param accountId 账号标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param knowledgeCardId 知识卡片标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("knowledgeCardId")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long countGoodByAccountIdAndCardId(@Param("accountId") Long accountId,
                                       @Param("knowledgeCardId") Long knowledgeCardId);
}
