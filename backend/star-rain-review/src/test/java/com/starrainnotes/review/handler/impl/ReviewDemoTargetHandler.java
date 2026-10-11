/*
 * ReviewDemoTargetHandler：审核模块的测试文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：ReviewDecisionContext、ReviewTargetHandler、ReviewTargetRef、ReviewTargetView、ReviewDemoRevisions。
 * 阅读入口：decidedStatus（决定状态处理入口）；targetModule（目标对象所属模块处理入口）；targetType（目标对象的业务类型处理入口）；supports（判断是否支持审核Demo目标处理器）。
 */

package com.starrainnotes.review.handler.impl;

import com.starrainnotes.review.api.ReviewDecisionContext;
import com.starrainnotes.review.api.ReviewTargetHandler;
import com.starrainnotes.review.api.ReviewTargetRef;
import com.starrainnotes.review.api.ReviewTargetView;
import com.starrainnotes.review.utils.ReviewDemoRevisions;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*
 * 审核 SPI 的测试夹具。仅供单元测试验证不可变视图与回调。
 *
 * 它同时是接入方的参考实现：
 *   1. targetModule / targetType 声明自己负责哪一类目标；
 *   2. loadReviewView 按冻结的 revisionRef 返回不可变快照，
 *      绝不读取「当前版本」——这正是 targetRevisionRef 存在的意义；
 *   3. onApproved / onRejected / onCanceled 只记录自己的状态推进结果，
 *      不反向调用 Review 改状态。
 *
 * 版本内容由 ReviewDemoRevisions 按 revisionRef 确定性推导，
 * 生产环境由 Tutorial 实现 SPI，本类不注册为 Bean。
 */
/**
 * 审核Demo目标处理器，属于审核模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
public class ReviewDemoTargetHandler implements ReviewTargetHandler {

    // 演示目标的模块与类型；正式接入方各自使用自己的常量
    /**
     * MODULE。
     */
    public static final String TARGET_MODULE = "DEMO";
    /**
     * 类型。
     */
    public static final String TARGET_TYPE = "DEMO_TARGET";
    /**
     * 类型。
     */
    public static final String REVIEW_TYPE = "demo.publish";

    // 回调结果：key 是 reviewRequestId，value 是业务模块自己推进后的状态
    /**
     * 决定Statuses。
     */
    private final Map<Long, String> decidedStatuses = new ConcurrentHashMap<>();

    // 测试与诊断用：查询某个审核请求最终触发了哪个回调
    /**
     * 决定状态处理入口。
     * 主要协作调用：<code>decidedStatuses.get</code>。
     *
     * @param reviewRequestId 审核请求标识；类型为 <code>Long</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    public String decidedStatus(Long reviewRequestId) {
        return decidedStatuses.get(reviewRequestId);
    }

    /**
     * 目标对象所属模块处理入口。
     * @return 类型为 <code>String</code> 的处理结果
     */
    @Override
    public String targetModule() {
        return TARGET_MODULE;
    }

    /**
     * 目标对象的业务类型处理入口。
     * @return 类型为 <code>String</code> 的处理结果
     */
    @Override
    public String targetType() {
        return TARGET_TYPE;
    }

    /**
     * 判断是否支持审核Demo目标处理器。
     * 主要协作调用：<code>TARGET_MODULE.equals</code>、<code>TARGET_TYPE.equals</code>、<code>REVIEW_TYPE.equals</code>。
     *
     * @param targetModule 目标对象所属模块；类型为 <code>String</code>
     * @param targetType 目标对象的业务类型；类型为 <code>String</code>
     * @param reviewType 审核类型；类型为 <code>String</code>
     * @return 本方法条件是否成立的判断结果
     */
    @Override
    public boolean supports(String targetModule, String targetType, String reviewType) {
        return TARGET_MODULE.equals(targetModule)
                && TARGET_TYPE.equals(targetType)
                && REVIEW_TYPE.equals(reviewType);
    }

    /**
     * 加载审核View。
     * 主要协作调用：<code>TARGET_MODULE.equals</code>。
     *
     * @param target 目标；类型为 <code>ReviewTargetRef</code>
     * @return 类型为 <code>ReviewTargetView</code> 的处理结果
     */
    @Override
    public ReviewTargetView loadReviewView(ReviewTargetRef target) {
        if (target == null || !TARGET_MODULE.equals(target.getTargetModule())) {
            return null;
        }
        String content = ReviewDemoRevisions.contentFor(target.getRevisionRef());
        if (content == null) {
            // 版本不可读时返回 null，由 Review 转成 REVIEW_TARGET_NOT_FOUND，
            // 而不是编造一个空视图骗过 Reviewer
            return null;
        }
        return ReviewDemoTargetView.builder()
                .viewType("DEMO_TARGET_SNAPSHOT")
                .targetId(target.getTargetId())
                .targetModule(target.getTargetModule())
                .targetType(target.getTargetType())
                .revisionRef(target.getRevisionRef())
                .title("演示目标 #" + target.getTargetId())
                .summary("这是提交审核时冻结的不可变快照，Reviewer 看到的永远是这一版。")
                .contentSnapshot(content)
                .frozenAt(ReviewDemoRevisions.frozenAt())
                .build();
    }

    /**
     * 响应已批准。
     * 主要协作调用：<code>decidedStatuses.put</code>。
     *
     * @param context context；类型为 <code>ReviewDecisionContext</code>
     */
    @Override
    public void onApproved(ReviewDecisionContext context) {
        // 真实业务模块在这里把目标推进到「已发布」；演示只记录结论
        decidedStatuses.put(context.getReviewRequestId(), "APPROVED");
    }

    /**
     * 响应已拒绝。
     * 主要协作调用：<code>decidedStatuses.put</code>。
     *
     * @param context context；类型为 <code>ReviewDecisionContext</code>
     */
    @Override
    public void onRejected(ReviewDecisionContext context) {
        // 真实业务模块在这里把目标退回「可编辑」，并把 reason 通知给申请人
        decidedStatuses.put(context.getReviewRequestId(), "REJECTED");
    }

    /**
     * 响应Canceled。
     * 主要协作调用：<code>decidedStatuses.put</code>。
     *
     * @param context context；类型为 <code>ReviewDecisionContext</code>
     */
    @Override
    public void onCanceled(ReviewDecisionContext context) {
        decidedStatuses.put(context.getReviewRequestId(), "CANCELED");
    }
}
