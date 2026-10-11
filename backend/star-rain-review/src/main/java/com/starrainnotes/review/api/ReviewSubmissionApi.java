package com.starrainnotes.review.api;

import com.starrainnotes.review.api.dto.ReviewSubmissionCommand;
import com.starrainnotes.review.api.dto.ReviewSubmissionResult;
import java.util.Optional;

/*
 * 业务模块提交/取消审核的唯一入口。
 *
 * 浏览器永远不应该直接调用这里：业务模块先验证 actor 与对象权限，
 * 确定不可变 revisionRef，再调用 submit()，最后推进自己的「审核中」状态。
 * 这样 targetModule/targetId 就不能被请求参数伪造。
 */
public interface ReviewSubmissionApi {

    // 提交审核。同一 target + reviewType 已有 PENDING 时抛 REVIEW_ALREADY_PENDING
    ReviewSubmissionResult submit(ReviewSubmissionCommand command);

    /*
     * 申请人取消自己提交的待审请求。
     * 对象级授权（actor 必须是 applicantAccountId）由 Service 校验。
     */
    void cancelByApplicant(Long reviewRequestId, Long actorAccountId);

    /*
     * 系统取消：目标对象被删除、配额回收等无自然人的场景。
     * 找不到 PENDING 时静默返回，系统动作不做幂等失败。
     */
    void cancelBySystem(ReviewTargetKey target, String reviewType, String reason);

    // 查询目标当前是否已有待审请求；不存在返回 Optional.empty()
    Optional<ReviewSummary> findActivePending(ReviewTargetKey target, String reviewType);
}
