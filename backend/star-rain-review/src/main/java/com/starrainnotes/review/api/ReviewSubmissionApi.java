/*
 * ReviewSubmissionApi：审核模块的跨模块接口文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 * 主要协作类型：ReviewSubmissionCommand、ReviewSubmissionResult。
 * 阅读入口：submit（提交审核Submission）；cancelByApplicant（取消依据申请人）；cancelBySystem（取消依据System）；findActivePending（查找有效待处理）。
 */

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
/**
 * 审核Submission接口，属于审核模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
public interface ReviewSubmissionApi {

    // 提交审核。同一 target + reviewType 已有 PENDING 时抛 REVIEW_ALREADY_PENDING
    /**
     * 提交审核Submission。
     *
     * @param command command；类型为 <code>ReviewSubmissionCommand</code>
     * @return 类型为 <code>ReviewSubmissionResult</code> 的处理结果
     */
    ReviewSubmissionResult submit(ReviewSubmissionCommand command);

    /*
     * 申请人取消自己提交的待审请求。
     * 对象级授权（actor 必须是 applicantAccountId）由 Service 校验。
     */
    /**
     * 取消依据申请人。
     *
     * @param reviewRequestId 审核请求标识；类型为 <code>Long</code>
     * @param actorAccountId 操作者账号标识；类型为 <code>Long</code>
     */
    void cancelByApplicant(Long reviewRequestId, Long actorAccountId);

    /*
     * 系统取消：目标对象被删除、配额回收等无自然人的场景。
     * 找不到 PENDING 时静默返回，系统动作不做幂等失败。
     */
    /**
     * 取消依据System。
     *
     * @param target 目标；类型为 <code>ReviewTargetKey</code>
     * @param reviewType 审核类型；类型为 <code>String</code>
     * @param reason 原因；类型为 <code>String</code>
     */
    void cancelBySystem(ReviewTargetKey target, String reviewType, String reason);

    // 查询目标当前是否已有待审请求；不存在返回 Optional.empty()
    /**
     * 查找有效待处理。
     *
     * @param target 目标；类型为 <code>ReviewTargetKey</code>
     * @param reviewType 审核类型；类型为 <code>String</code>
     * @return 可选结果；没有匹配值时由空 Optional 表达
     */
    Optional<ReviewSummary> findActivePending(ReviewTargetKey target, String reviewType);
}
