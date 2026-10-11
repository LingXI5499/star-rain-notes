/*
 * ReviewDecisionService：审核模块的业务契约文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：ReviewApproveCommand、ReviewRejectCommand。
 * 阅读入口：approve（批准审核决定）；reject（拒绝审核决定）；recoverApproved（recover已批准处理入口）。
 */

package com.starrainnotes.review.service;

import com.starrainnotes.review.dto.ReviewApproveCommand;
import com.starrainnotes.review.dto.ReviewRejectCommand;

/*
 * REV-004 / REV-005 服务接口。
 *
 * 两个方法都不返回 VO：审核动作的结果只有「成功」或「抛业务异常」两种。
 * 前端需要最新状态时应该重新拉详情，而不是信任写接口的返回值，
 * 这样并发审批的提示文案才能统一由 REVIEW_ALREADY_COMPLETED 驱动。
 */
/**
 * 审核决定业务服务，属于审核模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface ReviewDecisionService {

    /**
     * 批准审核决定。
     *
     * @param reviewRequestId 审核请求标识；类型为 <code>Long</code>
     * @param command command；类型为 <code>ReviewApproveCommand</code>
     * @param reviewerAccountId 审核人账号标识；类型为 <code>Long</code>
     */
    void approve(Long reviewRequestId, ReviewApproveCommand command, Long reviewerAccountId);

    /**
     * 拒绝审核决定。
     *
     * @param reviewRequestId 审核请求标识；类型为 <code>Long</code>
     * @param command command；类型为 <code>ReviewRejectCommand</code>
     * @param reviewerAccountId 审核人账号标识；类型为 <code>Long</code>
     */
    void reject(Long reviewRequestId, ReviewRejectCommand command, Long reviewerAccountId);
    /**
     * recover已批准处理入口。
     *
     * @param reviewRequestId 审核请求标识；类型为 <code>Long</code>
     * @param reviewerAccountId 审核人账号标识；类型为 <code>Long</code>
     */
    void recoverApproved(Long reviewRequestId, Long reviewerAccountId);
}
