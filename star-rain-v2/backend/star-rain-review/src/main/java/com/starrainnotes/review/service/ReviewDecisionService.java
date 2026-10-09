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
public interface ReviewDecisionService {

    void approve(Long reviewRequestId, ReviewApproveCommand command, Long reviewerAccountId);

    void reject(Long reviewRequestId, ReviewRejectCommand command, Long reviewerAccountId);
}
