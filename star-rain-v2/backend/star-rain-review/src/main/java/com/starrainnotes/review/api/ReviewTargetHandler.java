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
public interface ReviewTargetHandler {

    // 负责的目标模块，例如 TUTORIAL
    String targetModule();

    // 负责的目标对象类型，例如 CHAPTER
    String targetType();

    /*
     * 是否认领这个 reviewType。
     *
     * 默认实现「一个 targetModule + targetType 只对应一种审核」的常见情形：
     * 这种模块不需要写任何判断。若同一个目标类型将来出现多种审核，
     * 实现方必须覆盖本方法显式区分，否则 Review 会以
     * REVIEW_TARGET_NOT_SUPPORTED 拒绝提交，而不是把回调发给错误的模块。
     */
    default boolean supports(String targetModule, String targetType, String reviewType) {
        return targetModule().equals(targetModule) && targetType().equals(targetType);
    }

    /*
     * 按业务模块自己冻结的 revisionRef 返回审核视图。
     * 返回 null 表示该版本已不可读，Review 会以 REVIEW_TARGET_NOT_FOUND 拒绝详情请求。
     */
    ReviewTargetView loadReviewView(ReviewTargetRef target);

    // 回调默认空实现：接入方只覆盖自己关心的事件，避免为不用的事件写空方法
    default void onApproved(ReviewDecisionContext context) {
    }

    default void onRejected(ReviewDecisionContext context) {
    }

    default void onCanceled(ReviewDecisionContext context) {
    }
}
