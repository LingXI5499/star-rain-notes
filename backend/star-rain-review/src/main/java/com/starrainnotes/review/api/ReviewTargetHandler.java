/*
 * ReviewTargetHandler：审核模块的跨模块接口文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 * 阅读入口：targetModule（目标对象所属模块处理入口）；targetType（目标对象的业务类型处理入口）；supports（判断是否支持审核目标处理器）；loadReviewView（加载审核View）。
 */

package com.starrainnotes.review.api;

/*
 * 业务模块接入 Review 的唯一 SPI。
 *
 * Review 通过它做三件事：
 *   1. targetModule + targetType（可选 supports）—— 我是否负责这一类目标；
 *   2. loadReviewView                     —— 按冻结的 revisionRef 返回不可变审核视图；
 *   3. onApproved/onRejected/onCanceled    —— 审核结论回调，由业务模块自己推进状态。
 *
 * 三条铁律：
 *   1. Review 不注入任何业务模块的 Mapper，也不读业务表；
 *   2. 业务模块不直接操作 sr_review_request / sr_review_action；
 *   3. 回调在审核决定事务提交之后执行，业务模块派生动作失败不会回滚已生效的审核决定。
 *
 * 提交侧入口是 ReviewSubmissionApi，不在本接口内。
 */
/**
 * 审核目标处理器，属于审核模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
public interface ReviewTargetHandler {

    // 负责的目标模块，例如 TUTORIAL
    /**
     * 目标对象所属模块处理入口。
     * @return 类型为 <code>String</code> 的处理结果
     */
    String targetModule();

    // 负责的目标对象类型，例如 CHAPTER
    /**
     * 目标对象的业务类型处理入口。
     * @return 类型为 <code>String</code> 的处理结果
     */
    String targetType();

    /*
     * 是否认领这个 reviewType。
     *
     * 默认实现「一个 targetModule + targetType 只对应一种审核」的常见情形：
     * 这种模块不需要写任何判断。若同一个目标类型将来出现多种审核，
     * 实现方必须覆盖本方法显式区分，否则 Review 会以
     * REVIEW_TARGET_NOT_SUPPORTED 拒绝提交，而不是把回调发给错误的模块。
     */
    /**
     * 判断是否支持审核目标处理器。
     * 关键处理步骤调用 <code>targetModule</code>、<code>targetType</code>。
     *
     * @param targetModule 目标对象所属模块；类型为 <code>String</code>
     * @param targetType 目标对象的业务类型；类型为 <code>String</code>
     * @param reviewType 审核类型；类型为 <code>String</code>
     * @return 本方法条件是否成立的判断结果
     */
    default boolean supports(String targetModule, String targetType, String reviewType) {
        return targetModule().equals(targetModule) && targetType().equals(targetType);
    }

    /*
     * 按业务模块自己冻结的 revisionRef 返回审核视图。
     * 返回 null 表示该版本已不可读，Review 会以 REVIEW_TARGET_NOT_FOUND 拒绝详情请求。
     */
    /**
     * 加载审核View。
     *
     * @param target 目标；类型为 <code>ReviewTargetRef</code>
     * @return 类型为 <code>ReviewTargetView</code> 的处理结果
     */
    ReviewTargetView loadReviewView(ReviewTargetRef target);

    // 回调默认空实现：接入方只覆盖自己关心的事件，避免为不用的事件写空方法
    /**
     * 响应已批准。
     *
     * @param context context；类型为 <code>ReviewDecisionContext</code>
     */
    default void onApproved(ReviewDecisionContext context) {
    }

    /**
     * 响应已拒绝。
     *
     * @param context context；类型为 <code>ReviewDecisionContext</code>
     */
    default void onRejected(ReviewDecisionContext context) {
    }

    /**
     * 响应Canceled。
     *
     * @param context context；类型为 <code>ReviewDecisionContext</code>
     */
    default void onCanceled(ReviewDecisionContext context) {
    }

    /**
     * 判断是否支持已批准Recovery。
     * @return 本方法条件是否成立的判断结果
     */
    default boolean supportsApprovedRecovery() { return false; }

    /**
     * recover已批准处理入口。
     * 实现中的显式失败类型包括 <code>com.starrainnotes.review.exception.ReviewAccessDeniedException</code>；具体触发条件见对应分支。
     *
     * @param context context；类型为 <code>ReviewDecisionContext</code>
     */
    default void recoverApproved(ReviewDecisionContext context) {
        throw new com.starrainnotes.review.exception.ReviewAccessDeniedException();
    }
}
