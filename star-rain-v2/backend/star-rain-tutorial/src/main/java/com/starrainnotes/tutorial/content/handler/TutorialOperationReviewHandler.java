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
        return new OperationView(target.getRevisionRef(), row.getDisplayName(), row.getOperation(), row.getStatus(),
                service.read(row.getBaselineJson()), service.read(row.getArgumentsJson()));
    }
    @Override
    public void onApproved(ReviewDecisionContext context) {
        try { service.approve(context.getTarget().getTargetId()); }
        catch (RuntimeException exception) {
            service.finish(context.getTarget().getTargetId(), "FAILED");
            throw exception;
        }
    }
    @Override
    public void onRejected(ReviewDecisionContext context) { service.finish(context.getTarget().getTargetId(), "REJECTED"); }
    @Override
    public void onCanceled(ReviewDecisionContext context) { service.finish(context.getTarget().getTargetId(), "CANCELED"); }

    @Data
    @AllArgsConstructor
    public static class OperationView implements ReviewTargetView {
        private String revisionRef;
        private String title;
        private String operation;
        private String executionStatus;
        private JsonNode before;
        private JsonNode proposed;
        @Override @JsonProperty("viewType")
        public String viewType() { return "TUTORIAL_OPERATION"; }
    }
}
