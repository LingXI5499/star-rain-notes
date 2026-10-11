/*
 * ReviewActionMapper：审核模块的数据访问文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：ReviewActionEntity。
 * 阅读入口：insertAction（写入动作）；selectByReviewRequestId（按审核请求标识读取查询）。
 */

package com.starrainnotes.review.mapper;

import com.starrainnotes.review.entity.ReviewActionEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/*
 * 审核动作历史数据访问。
 *
 * 只插入与查询，没有 update / delete：历史一旦写下就是事实，
 * 允许修改历史等于允许篡改审核轨迹。
 */
/**
 * 审核动作数据映射，属于审核模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
@Mapper
public interface ReviewActionMapper {

    /**
     * 写入动作。
     *
     * 持久化操作：新增，涉及 <code>sr_review_action</code>。
     *
     * @param action 动作；类型为 <code>ReviewActionEntity</code>
     * @return SQL 写入影响的记录行数
     */
    int insertAction(ReviewActionEntity action);

    /**
     * 按审核请求标识读取查询。
     *
     * 持久化操作：读取，涉及 <code>sr_review_action</code>。
     *
     * @param reviewRequestId 审核请求标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("reviewRequestId")</code>
     * @return 本次处理得到的条目集合
     */
    List<ReviewActionEntity> selectByReviewRequestId(@Param("reviewRequestId") Long reviewRequestId);
}
