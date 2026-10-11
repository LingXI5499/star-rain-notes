package com.starrainnotes.review.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.review.api.ReviewDecisionContext;
import com.starrainnotes.review.api.ReviewTargetHandler;
import com.starrainnotes.review.api.ReviewTargetRef;
import com.starrainnotes.review.api.ReviewTargetView;
import com.starrainnotes.review.dto.ReviewApproveCommand;
import com.starrainnotes.review.dto.ReviewRejectCommand;
import com.starrainnotes.review.entity.ReviewActionEntity;
import com.starrainnotes.review.entity.ReviewRequestEntity;
import com.starrainnotes.review.enumeration.ReviewActionType;
import com.starrainnotes.review.enumeration.ReviewStatus;
import com.starrainnotes.review.handler.ReviewDecisionCallbackDispatcher;
import com.starrainnotes.review.handler.impl.ReviewDemoTargetHandler;
import com.starrainnotes.review.handler.impl.ReviewTargetHandlerRegistryImpl;
import com.starrainnotes.review.mapper.ReviewActionMapper;
import com.starrainnotes.review.mapper.ReviewRequestMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/*
 * REV-004 审核通过 / REV-005 审核拒绝的单元测试。
 *
 * 两条最关键的行为：
 *   1. 决定是否生效只看条件 UPDATE 的 affected_rows，0 行必须是 REVIEW_ALREADY_COMPLETED，
 *      这就是「两个 Reviewer 并发只能成功一个」的落点；
 *   2. SPI 回调必须在事务提交之后触发，因此测试里注册真实的事务同步，
 *      断言「方法返回时还没回调」，再手工执行 afterCommit 才看到回调。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReviewDecisionServiceImplTest {

    @Mock
    private ReviewRequestMapper requestMapper;

    @Mock
    private ReviewActionMapper actionMapper;

    private final ReviewDecisionCallbackDispatcher dispatcher = new ReviewDecisionCallbackDispatcher();

    @AfterEach
    void clearSynchronization() {
        // 用例里手工开启过事务同步，必须清理，否则会污染后续用例
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    /*
     * 记录回调的测试 Handler：把 onApproved / onRejected / onCanceled 收到的上下文存下来，
     * 用来断言「回调被触发」以及「回调载荷带的是审核决定后的真实数据」。
     *
     * module / type / claimedReviewType 全部可配置，这样「多个 Handler 抢同一目标类型」
     * 这类路由冲突也能在单测里构造出来。
     */
    private static class RecordingHandler implements ReviewTargetHandler {

        private final String module;
        private final String type;
        private final String claimedReviewType;

        private final List<ReviewDecisionContext> approved = new ArrayList<>();
        private final List<ReviewDecisionContext> rejected = new ArrayList<>();
        private final List<ReviewDecisionContext> canceled = new ArrayList<>();

        RecordingHandler() {
            this(ReviewDemoTargetHandler.TARGET_MODULE, ReviewDemoTargetHandler.TARGET_TYPE,
                    ReviewDemoTargetHandler.REVIEW_TYPE);
        }

        // claimedReviewType 传 null 表示「只按 module + type 认领」，与接口默认实现同义
        RecordingHandler(String module, String type, String claimedReviewType) {
            this.module = module;
            this.type = type;
            this.claimedReviewType = claimedReviewType;
        }

        @Override
        public String targetModule() {
            return module;
        }

        @Override
        public String targetType() {
            return type;
        }

        @Override
        public boolean supports(String targetModule, String targetType, String reviewType) {
            return module.equals(targetModule) && type.equals(targetType)
                    && (claimedReviewType == null || claimedReviewType.equals(reviewType));
        }

        @Override
        public ReviewTargetView loadReviewView(ReviewTargetRef target) {
            return null;
        }

        @Override
        public void onApproved(ReviewDecisionContext context) {
            approved.add(context);
        }

        @Override
        public void onRejected(ReviewDecisionContext context) {
            rejected.add(context);
        }

        @Override
        public void onCanceled(ReviewDecisionContext context) {
            canceled.add(context);
        }
    }

    private ReviewDecisionServiceImpl service(ReviewTargetHandler handler) {
        return new ReviewDecisionServiceImpl(requestMapper, actionMapper,
                new ReviewTargetHandlerRegistryImpl(List.of(handler)), dispatcher);
    }

    private static ReviewRequestEntity pendingRequest() {
        ReviewRequestEntity entity = new ReviewRequestEntity();
        entity.setId(500L);
        entity.setReviewType(ReviewDemoTargetHandler.REVIEW_TYPE);
        entity.setTargetModule(ReviewDemoTargetHandler.TARGET_MODULE);
        entity.setTargetType(ReviewDemoTargetHandler.TARGET_TYPE);
        entity.setTargetId(100L);
        entity.setTargetRevisionRef("revision:7");
        entity.setTargetDisplayName("《Java 程序设计》");
        entity.setApplicantAccountId(9L);
        entity.setApplicantDisplayName("账户 #9");
        entity.setStatus(ReviewStatus.PENDING_CODE);
        entity.setSubmittedAt(LocalDateTime.now());
        return entity;
    }

    // 手工执行已注册的 afterCommit 回调，模拟事务提交成功
    private static void commitTransaction() {
        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();
        for (TransactionSynchronization synchronization : synchronizations) {
            synchronization.afterCommit();
        }
    }

    @Test
    @DisplayName("审核通过：条件更新生效、写 APPROVED 动作，回调在提交之后才触发")
    void approveUpdatesAndCallbacksAfterCommit() {
        RecordingHandler handler = new RecordingHandler();
        ReviewDecisionServiceImpl service = service(handler);
        when(requestMapper.requestById(500L)).thenReturn(pendingRequest());
        when(requestMapper.approveIfPending(eq(500L), eq(1L), eq("审核通过"), any(LocalDateTime.class)))
                .thenReturn(1);
        TransactionSynchronizationManager.initSynchronization();

        service.approve(500L, ReviewApproveCommand.builder().note("审核通过").build(), 1L);

        ArgumentCaptor<ReviewActionEntity> actionCaptor = ArgumentCaptor.forClass(ReviewActionEntity.class);
        verify(actionMapper).insertAction(actionCaptor.capture());
        assertThat(actionCaptor.getValue().getActionType()).isEqualTo(ReviewActionType.APPROVED_CODE);
        assertThat(actionCaptor.getValue().getActorAccountId()).isEqualTo(1L);

        // 关键断言：方法返回时目标模块还没有被通知
        assertThat(handler.approved).isEmpty();

        commitTransaction();

        assertThat(handler.approved).hasSize(1);
        ReviewDecisionContext context = handler.approved.get(0);
        assertThat(context.getReviewRequestId()).isEqualTo(500L);
        assertThat(context.getStatus()).isEqualTo(ReviewStatus.APPROVED_CODE);
        assertThat(context.getActionType()).isEqualTo(ReviewActionType.APPROVED);
        assertThat(context.getTarget().getRevisionRef()).isEqualTo("revision:7");
        assertThat(context.getReviewerAccountId()).isEqualTo(1L);
        assertThat(context.getApplicantAccountId()).isEqualTo(9L);
    }

    @Test
    @DisplayName("重复通过同一条请求：条件更新 0 行 → REVIEW_ALREADY_COMPLETED，不写历史也不回调")
    void approveRejectsRepeatDecision() {
        RecordingHandler handler = new RecordingHandler();
        ReviewDecisionServiceImpl service = service(handler);
        when(requestMapper.requestById(500L)).thenReturn(pendingRequest());
        // 已被另一个 Reviewer 处理，条件更新影响 0 行
        when(requestMapper.approveIfPending(anyLong(), anyLong(), any(), any(LocalDateTime.class)))
                .thenReturn(0);

        assertThatThrownBy(() -> service.approve(500L, new ReviewApproveCommand(), 1L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("REVIEW_ALREADY_COMPLETED");

        verify(actionMapper, never()).insertAction(any());
        assertThat(handler.approved).isEmpty();
    }

    @Test
    @DisplayName("并发下另一个 Reviewer 先拒绝：本次通过必须失败，最终只有一个终态")
    void approveLosesRaceAgainstReject() {
        RecordingHandler handler = new RecordingHandler();
        ReviewDecisionServiceImpl service = service(handler);
        when(requestMapper.requestById(500L)).thenReturn(pendingRequest());
        when(requestMapper.approveIfPending(anyLong(), anyLong(), any(), any(LocalDateTime.class)))
                .thenReturn(0);

        assertThatThrownBy(() -> service.approve(500L, new ReviewApproveCommand(), 1L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("REVIEW_ALREADY_COMPLETED");
        // 失败的一方不许触发任何业务回调
        assertThat(handler.approved).isEmpty();
        assertThat(handler.rejected).isEmpty();
    }

    @Test
    @DisplayName("审核拒绝：原因必填，缺失时 REVIEW_REJECT_REASON_REQUIRED 且不更新数据库")
    void rejectRequiresReason() {
        RecordingHandler handler = new RecordingHandler();
        ReviewDecisionServiceImpl service = service(handler);
        when(requestMapper.requestById(500L)).thenReturn(pendingRequest());

        assertThatThrownBy(() -> service.reject(500L, ReviewRejectCommand.builder().reason("   ").build(), 1L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("REVIEW_REJECT_REASON_REQUIRED");

        verify(requestMapper, never()).rejectIfPending(anyLong(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("审核拒绝成功：拒绝原因写入决定与动作历史，回调在提交之后触发")
    void rejectWritesReasonAndCallbacksAfterCommit() {
        RecordingHandler handler = new RecordingHandler();
        ReviewDecisionServiceImpl service = service(handler);
        when(requestMapper.requestById(500L)).thenReturn(pendingRequest());
        when(requestMapper.rejectIfPending(eq(500L), eq(1L), eq("示例代码无法运行"),
                any(LocalDateTime.class))).thenReturn(1);
        TransactionSynchronizationManager.initSynchronization();

        service.reject(500L, ReviewRejectCommand.builder().reason("示例代码无法运行").build(), 1L);

        ArgumentCaptor<ReviewActionEntity> actionCaptor = ArgumentCaptor.forClass(ReviewActionEntity.class);
        verify(actionMapper).insertAction(actionCaptor.capture());
        assertThat(actionCaptor.getValue().getActionType()).isEqualTo(ReviewActionType.REJECTED_CODE);
        assertThat(actionCaptor.getValue().getNote()).isEqualTo("示例代码无法运行");
        assertThat(handler.rejected).isEmpty();

        commitTransaction();

        assertThat(handler.rejected).hasSize(1);
        assertThat(handler.rejected.get(0).getReason()).isEqualTo("示例代码无法运行");
        assertThat(handler.rejected.get(0).getStatus()).isEqualTo(ReviewStatus.REJECTED_CODE);
    }

    @Test
    @DisplayName("重复拒绝同一条请求：REVIEW_ALREADY_COMPLETED")
    void rejectRejectsRepeatDecision() {
        RecordingHandler handler = new RecordingHandler();
        ReviewDecisionServiceImpl service = service(handler);
        when(requestMapper.requestById(500L)).thenReturn(pendingRequest());
        when(requestMapper.rejectIfPending(anyLong(), anyLong(), any(), any(LocalDateTime.class)))
                .thenReturn(0);

        assertThatThrownBy(() -> service.reject(500L,
                ReviewRejectCommand.builder().reason("不合格").build(), 1L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("REVIEW_ALREADY_COMPLETED");

        assertThat(handler.rejected).isEmpty();
    }

    @Test
    @DisplayName("审核不存在的请求：REVIEW_NOT_FOUND")
    void decisionOnUnknownRequest() {
        RecordingHandler handler = new RecordingHandler();
        ReviewDecisionServiceImpl service = service(handler);
        when(requestMapper.requestById(404L)).thenReturn(null);

        assertThatThrownBy(() -> service.approve(404L, new ReviewApproveCommand(), 1L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("REVIEW_NOT_FOUND");
    }

    @Test
    @DisplayName("决策人缺失：REVIEW_ACCESS_DENIED，不能出现没有责任人的审核决定")
    void decisionRequiresReviewer() {
        RecordingHandler handler = new RecordingHandler();
        ReviewDecisionServiceImpl service = service(handler);

        assertThatThrownBy(() -> service.approve(500L, new ReviewApproveCommand(), null))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("REVIEW_ACCESS_DENIED");

        verify(requestMapper, never()).approveIfPending(anyLong(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("目标模块没有接单：审核在写库之前就失败，避免出现没人处理的 APPROVED")
    void decisionRequiresRegisteredHandler() {
        ReviewDecisionServiceImpl service = new ReviewDecisionServiceImpl(requestMapper, actionMapper,
                new ReviewTargetHandlerRegistryImpl(List.of()), dispatcher);
        when(requestMapper.requestById(500L)).thenReturn(pendingRequest());

        assertThatThrownBy(() -> service.approve(500L, new ReviewApproveCommand(), 1L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("REVIEW_TARGET_NOT_SUPPORTED");

        verify(requestMapper, never()).approveIfPending(anyLong(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("没有活动事务时回调立即执行：回调不能被静默丢弃")
    void callbackRunsImmediatelyWithoutTransaction() {
        RecordingHandler handler = new RecordingHandler();
        ReviewDecisionServiceImpl service = service(handler);
        when(requestMapper.requestById(500L)).thenReturn(pendingRequest());
        when(requestMapper.approveIfPending(anyLong(), anyLong(), any(), any(LocalDateTime.class)))
                .thenReturn(1);

        service.approve(500L, new ReviewApproveCommand(), 1L);

        assertThat(handler.approved).hasSize(1);
    }

    @Test
    @DisplayName("注册表严格按声明的 reviewType 路由：无人认领报 NOT_SUPPORTED，重复认领报 DECISION_CONFLICT")
    void registryRoutingRules() {
        RecordingHandler lone = new RecordingHandler();
        ReviewTargetHandlerRegistryImpl single = new ReviewTargetHandlerRegistryImpl(List.of(lone));

        assertThat(single.supports(ReviewDemoTargetHandler.TARGET_MODULE,
                ReviewDemoTargetHandler.TARGET_TYPE, ReviewDemoTargetHandler.REVIEW_TYPE)).isTrue();
        assertThat(single.supports("TUTORIAL", ReviewDemoTargetHandler.TARGET_TYPE,
                ReviewDemoTargetHandler.REVIEW_TYPE)).isFalse();
        // 同一目标类型下未声明的 reviewType 不做宽松兜底，宁可拒绝也不猜
        assertThat(single.supports(ReviewDemoTargetHandler.TARGET_MODULE,
                ReviewDemoTargetHandler.TARGET_TYPE, "demo.archive")).isFalse();
        assertThatThrownBy(() -> single.require(ReviewDemoTargetHandler.TARGET_MODULE,
                ReviewDemoTargetHandler.TARGET_TYPE, "demo.archive"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("REVIEW_TARGET_NOT_SUPPORTED");

        // 两个 Handler 抢同一个 (module, type, reviewType) 属于接入冲突，必须报错而不是随便挑
        RecordingHandler duplicated = new RecordingHandler();
        ReviewTargetHandlerRegistryImpl ambiguous =
                new ReviewTargetHandlerRegistryImpl(List.of(duplicated, new RecordingHandler()));
        assertThatThrownBy(() -> ambiguous.require(ReviewDemoTargetHandler.TARGET_MODULE,
                ReviewDemoTargetHandler.TARGET_TYPE, ReviewDemoTargetHandler.REVIEW_TYPE))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("REVIEW_DECISION_CONFLICT");
    }
}
