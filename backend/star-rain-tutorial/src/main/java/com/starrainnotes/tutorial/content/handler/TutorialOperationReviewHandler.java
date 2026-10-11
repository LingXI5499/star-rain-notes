/*
 * TutorialOperationReviewHandler：教程与学习模块的模块支持文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 * 主要协作类型：ReviewDecisionContext、ReviewTargetHandler、ReviewTargetRef、ReviewTargetView、TutorialOperationMapper、TutorialOperationService。
 * 阅读入口：targetModule（目标对象所属模块处理入口）；targetType（目标对象的业务类型处理入口）；supports（判断是否支持教程操作审核处理器）；loadReviewView（加载审核View）。
 */

package com.starrainnotes.tutorial.content.handler;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.starrainnotes.review.api.ReviewDecisionContext;
import com.starrainnotes.review.api.ReviewTargetHandler;
import com.starrainnotes.review.api.ReviewTargetRef;
import com.starrainnotes.review.api.ReviewTargetView;
import com.starrainnotes.tutorial.content.mapper.TutorialOperationMapper;
import com.starrainnotes.tutorial.content.service.TutorialOperationService;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Lazy;

/**
 * 教程操作审核处理器，属于教程与学习模块。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 */
@Component
public class TutorialOperationReviewHandler implements ReviewTargetHandler {
    /**
     * operations。
     * 协作类型：<code>TutorialOperationMapper</code>。
     */
    private final TutorialOperationMapper operations;
    /**
     * 业务服务。
     * 协作类型：<code>TutorialOperationService</code>。
     */
    private final TutorialOperationService service;

    /**
     * 构造TutorialOperationReviewHandler，接收本类使用的依赖或初始化参数。
     *
     * @param operations operations；类型为 <code>TutorialOperationMapper</code>
     * @param service 业务服务；类型为 <code>TutorialOperationService</code>
     */
    public TutorialOperationReviewHandler(TutorialOperationMapper operations,
                                         @Lazy TutorialOperationService service) {
        this.operations = operations;
        // 审核注册表在提交服务初始化时加载处理器，执行服务留到审核回调时获取。
        this.service = service;
    }
    /**
     * 目标对象所属模块处理入口。
     * @return 类型为 <code>String</code> 的处理结果
     */
    public String targetModule() { return "TUTORIAL"; }
    /**
     * 目标对象的业务类型处理入口。
     * @return 类型为 <code>String</code> 的处理结果
     */
    public String targetType() { return "OPERATION"; }
    /**
     * 判断是否支持教程操作审核处理器。
     *
     * @param module module；类型为 <code>String</code>
     * @param type 类型；类型为 <code>String</code>
     * @param reviewType 审核类型；类型为 <code>String</code>
     * @return 本方法条件是否成立的判断结果
     */
    @Override
    public boolean supports(String module, String type, String reviewType) {
        return "TUTORIAL".equals(module) && "OPERATION".equals(type) && "tutorial.operation".equals(reviewType);
    }
    /**
     * 加载审核View。
     * 主要协作调用：<code>operations.selectById</code>、<code>service.read</code>。
     *
     * @param target 目标；类型为 <code>ReviewTargetRef</code>
     * @return 类型为 <code>ReviewTargetView</code> 的处理结果
     */
    @Override
    public ReviewTargetView loadReviewView(ReviewTargetRef target) {
        var row = operations.selectById(target.getTargetId());
        if (row == null || !("tutorial-operation:" + row.getId()).equals(target.getRevisionRef())) return null;
        var view = new OperationView(target.getRevisionRef(), row.getDisplayName(), row.getOperation(), row.getStatus(),
                service.read(row.getBaselineJson()), service.read(row.getArgumentsJson()), row.getExecutionMessage());
        return view;
    }
    /**
     * 响应已批准。
     * 主要协作调用：<code>service.approve</code>、<code>service.recordFailure</code>。
     *
     * @param context context；类型为 <code>ReviewDecisionContext</code>
     */
    @Override
    public void onApproved(ReviewDecisionContext context) {
        try { service.approve(context.getTarget().getTargetId()); }
        catch (RuntimeException exception) {
            service.recordFailure(context.getTarget().getTargetId(), exception);
            throw exception;
        }
    }
    /**
     * 响应已拒绝。
     * 主要协作调用：<code>service.finish</code>。
     *
     * @param context context；类型为 <code>ReviewDecisionContext</code>
     */
    @Override
    public void onRejected(ReviewDecisionContext context) { service.finish(context.getTarget().getTargetId(), "REJECTED"); }
    /**
     * 响应Canceled。
     * 主要协作调用：<code>service.finish</code>。
     *
     * @param context context；类型为 <code>ReviewDecisionContext</code>
     */
    @Override
    public void onCanceled(ReviewDecisionContext context) { service.finish(context.getTarget().getTargetId(), "CANCELED"); }

    /**
     * 判断是否支持已批准Recovery。
     * @return 本方法条件是否成立的判断结果
     */
    @Override public boolean supportsApprovedRecovery() { return true; }
    /**
     * recover已批准处理入口。
     * 主要协作调用：<code>service.recoverApproved</code>、<code>service.recordFailure</code>。
     * 实现中的显式失败类型包括 <code>com.starrainnotes.tutorial.content.exception.TutorialStateException</code>；具体触发条件见对应分支。
     *
     * @param context context；类型为 <code>ReviewDecisionContext</code>
     */
    @Override public void recoverApproved(ReviewDecisionContext context) {
        Long id = context.getTarget().getTargetId();
        if (!"APPROVED".equals(context.getStatus()) || !("tutorial-operation:" + id).equals(context.getTarget().getRevisionRef())) {
            throw new com.starrainnotes.tutorial.content.exception.TutorialStateException("审核目标或审批结论不匹配");
        }
        try { service.recoverApproved(id); }
        catch (RuntimeException exception) { service.recordFailure(id, exception); throw exception; }
    }

    /**
     * 操作View，属于教程与学习模块。
     * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
     */
    @Data
    @AllArgsConstructor
    public static class OperationView implements ReviewTargetView {
        /**
         * 版本或修订的引用值。
         */
        private String revisionRef;
        /**
         * 标题。
         */
        private String title;
        /**
         * 操作。
         */
        private String operation;
        /**
         * 执行状态。
         */
        private String executionStatus;
        /**
         * 之前。
         */
        private JsonNode before;
        /**
         * proposed。
         */
        private JsonNode proposed;
        /**
         * 执行留言。
         */
        private String executionMessage;
        /**
         * view类型处理入口。
         * @return 类型为 <code>String</code> 的处理结果
         */
        @Override @JsonProperty("viewType")
        public String viewType() { return "TUTORIAL_OPERATION"; }
    }
}
