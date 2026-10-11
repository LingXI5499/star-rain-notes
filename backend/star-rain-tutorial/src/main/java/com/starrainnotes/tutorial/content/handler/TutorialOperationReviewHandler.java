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

@Component
public class TutorialOperationReviewHandler implements ReviewTargetHandler {
    private final TutorialOperationMapper operations;
    private final TutorialOperationService service;

    public TutorialOperationReviewHandler(TutorialOperationMapper operations,
                                         @Lazy TutorialOperationService service) {
        this.operations = operations;
        // 审核注册表在提交服务初始化时加载处理器，执行服务留到审核回调时获取。
        this.service = service;
    }
    public String targetModule() { return "TUTORIAL"; }
    public String targetType() { return "OPERATION"; }
    @Override
    public boolean supports(String module, String type, String reviewType) {
        return "TUTORIAL".equals(module) && "OPERATION".equals(type) && "tutorial.operation".equals(reviewType);
    }
    @Override
    public ReviewTargetView loadReviewView(ReviewTargetRef target) {
        var row = operations.selectById(target.getTargetId());
        if (row == null || !("tutorial-operation:" + row.getId()).equals(target.getRevisionRef())) return null;
        var view = new OperationView(target.getRevisionRef(), row.getDisplayName(), row.getOperation(), row.getStatus(),
                service.read(row.getBaselineJson()), service.read(row.getArgumentsJson()), row.getExecutionMessage());
        return view;
    }
    @Override
    public void onApproved(ReviewDecisionContext context) {
        try { service.approve(context.getTarget().getTargetId()); }
        catch (RuntimeException exception) {
            service.recordFailure(context.getTarget().getTargetId(), exception);
            throw exception;
        }
    }
    @Override
    public void onRejected(ReviewDecisionContext context) { service.finish(context.getTarget().getTargetId(), "REJECTED"); }
    @Override
    public void onCanceled(ReviewDecisionContext context) { service.finish(context.getTarget().getTargetId(), "CANCELED"); }

    @Override public boolean supportsApprovedRecovery() { return true; }
    @Override public void recoverApproved(ReviewDecisionContext context) {
        Long id = context.getTarget().getTargetId();
        if (!"APPROVED".equals(context.getStatus()) || !("tutorial-operation:" + id).equals(context.getTarget().getRevisionRef())) {
            throw new com.starrainnotes.tutorial.content.exception.TutorialStateException("审核目标或审批结论不匹配");
        }
        try { service.recoverApproved(id); }
        catch (RuntimeException exception) { service.recordFailure(id, exception); throw exception; }
    }

    @Data
    @AllArgsConstructor
    public static class OperationView implements ReviewTargetView {
        private String revisionRef;
        private String title;
        private String operation;
        private String executionStatus;
        private JsonNode before;
        private JsonNode proposed;
        private String executionMessage;
        @Override @JsonProperty("viewType")
        public String viewType() { return "TUTORIAL_OPERATION"; }
    }
}
