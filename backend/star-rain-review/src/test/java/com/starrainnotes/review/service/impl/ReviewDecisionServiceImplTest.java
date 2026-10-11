/*
 * ReviewDecisionServiceImplTest：审核模块的测试文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：ApiException、ReviewDecisionContext、ReviewTargetHandler、ReviewTargetRef、ReviewTargetView、ReviewApproveCommand、ReviewRejectCommand。
 * 阅读入口：targetModule（目标对象所属模块处理入口）；targetType（目标对象的业务类型处理入口）；supports（判断是否支持Recording处理器）；loadReviewView（加载审核View）。
 */

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
/**
 * 审核决定业务服务实现测试，属于审核模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReviewDecisionServiceImplTest {

    /**
     * 请求数据映射。
     * 协作类型：<code>ReviewRequestMapper</code>。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock
    private ReviewRequestMapper requestMapper;

    /**
     * 动作数据映射。
     * 协作类型：<code>ReviewActionMapper</code>。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock
    private ReviewActionMapper actionMapper;

    /**
     * dispatcher。
     */
    private final ReviewDecisionCallbackDispatcher dispatcher = new ReviewDecisionCallbackDispatcher();

    /**
     * 清理Synchronization。
     * 关键处理步骤调用 <code>TransactionSynchronizationManager.isSynchronizationActive</code>、<code>TransactionSynchronizationManager.clearSynchronization</code>。
     */
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
    /**
     * Recording处理器，属于审核模块。
     * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
     */
    private static class RecordingHandler implements ReviewTargetHandler {

        /**
         * module。
         */
        private final String module;
        /**
         * 类型。
         */
        private final String type;
        /**
         * claimed审核类型。
         */
        private final String claimedReviewType;

        /**
         * 已批准。
         */
        private final List<ReviewDecisionContext> approved = new ArrayList<>();
        /**
         * 已拒绝。
         */
        private final List<ReviewDecisionContext> rejected = new ArrayList<>();
        /**
         * canceled。
         */
        private final List<ReviewDecisionContext> canceled = new ArrayList<>();

        /**
         * 构造RecordingHandler，接收本类使用的依赖或初始化参数。
         * 关键处理步骤调用 <code>this</code>。
         */
        RecordingHandler() {
            this(ReviewDemoTargetHandler.TARGET_MODULE, ReviewDemoTargetHandler.TARGET_TYPE,
                    ReviewDemoTargetHandler.REVIEW_TYPE);
        }

        // claimedReviewType 传 null 表示「只按 module + type 认领」，与接口默认实现同义
        /**
         * 构造RecordingHandler，接收本类使用的依赖或初始化参数。
         *
         * @param module module；类型为 <code>String</code>
         * @param type 类型；类型为 <code>String</code>
         * @param claimedReviewType claimed审核类型；类型为 <code>String</code>
         */
        RecordingHandler(String module, String type, String claimedReviewType) {
            this.module = module;
            this.type = type;
            this.claimedReviewType = claimedReviewType;
        }

        /**
         * 目标对象所属模块处理入口。
         * @return 类型为 <code>String</code> 的处理结果
         */
        @Override
        public String targetModule() {
            return module;
        }

        /**
         * 目标对象的业务类型处理入口。
         * @return 类型为 <code>String</code> 的处理结果
         */
        @Override
        public String targetType() {
            return type;
        }

        /**
         * 判断是否支持Recording处理器。
         * 主要协作调用：<code>module.equals</code>、<code>type.equals</code>、<code>claimedReviewType.equals</code>。
         *
         * @param targetModule 目标对象所属模块；类型为 <code>String</code>
         * @param targetType 目标对象的业务类型；类型为 <code>String</code>
         * @param reviewType 审核类型；类型为 <code>String</code>
         * @return 本方法条件是否成立的判断结果
         */
        @Override
        public boolean supports(String targetModule, String targetType, String reviewType) {
            return module.equals(targetModule) && type.equals(targetType)
                    && (claimedReviewType == null || claimedReviewType.equals(reviewType));
        }

        /**
         * 加载审核View。
         *
         * @param target 目标；类型为 <code>ReviewTargetRef</code>
         * @return 类型为 <code>ReviewTargetView</code> 的处理结果
         */
        @Override
        public ReviewTargetView loadReviewView(ReviewTargetRef target) {
            return null;
        }

        /**
         * 响应已批准。
         * 主要协作调用：<code>approved.add</code>。
         *
         * @param context context；类型为 <code>ReviewDecisionContext</code>
         */
        @Override
        public void onApproved(ReviewDecisionContext context) {
            approved.add(context);
        }

        /**
         * 响应已拒绝。
         * 主要协作调用：<code>rejected.add</code>。
         *
         * @param context context；类型为 <code>ReviewDecisionContext</code>
         */
        @Override
        public void onRejected(ReviewDecisionContext context) {
            rejected.add(context);
        }

        /**
         * 响应Canceled。
         * 主要协作调用：<code>canceled.add</code>。
         *
         * @param context context；类型为 <code>ReviewDecisionContext</code>
         */
        @Override
        public void onCanceled(ReviewDecisionContext context) {
            canceled.add(context);
        }
    }

    /**
     * 业务服务处理入口。
     * 关键处理步骤调用 <code>List.of</code>。
     *
     * @param handler 处理器；类型为 <code>ReviewTargetHandler</code>
     * @return 类型为 <code>ReviewDecisionServiceImpl</code> 的处理结果
     */
    private ReviewDecisionServiceImpl service(ReviewTargetHandler handler) {
        return new ReviewDecisionServiceImpl(requestMapper, actionMapper,
                new ReviewTargetHandlerRegistryImpl(List.of(handler)), dispatcher);
    }

    /**
     * 待处理请求处理入口。
     * 关键处理步骤调用 <code>entity.setId</code>、<code>entity.setReviewType</code>、<code>entity.setTargetModule</code>、<code>entity.setTargetType</code>。
     * @return 类型为 <code>ReviewRequestEntity</code> 的处理结果
     */
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
    /**
     * 提交事务。
     * 关键处理步骤调用 <code>TransactionSynchronizationManager.getSynchronizations</code>、<code>synchronization.afterCommit</code>。
     */
    private static void commitTransaction() {
        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();
        for (TransactionSynchronization synchronization : synchronizations) {
            synchronization.afterCommit();
        }
    }

    /**
     * \u5ba1\u6838\u901a\u8fc7\uff1a\u6761\u4ef6\u66f4\u65b0\u751f\u6548\u3001\u5199 APPROVED \u52a8\u4f5c\uff0c\u56de\u8c03\u5728\u63d0\u4ea4\u4e4b\u540e\u624d\u89e6\u53d1。
     * 主要协作调用：<code>requestMapper.requestById</code>、<code>requestMapper.approveIfPending</code>。
     */
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

    /**
     * \u91cd\u590d\u901a\u8fc7\u540c\u4e00\u6761\u8bf7\u6c42\uff1a\u6761\u4ef6\u66f4\u65b0 0 \u884c \u2192 REVIEW_ALREADY_COMPLETED\uff0c\u4e0d\u5199\u5386\u53f2\u4e5f\u4e0d\u56de\u8c03。
     * 主要协作调用：<code>requestMapper.requestById</code>、<code>requestMapper.approveIfPending</code>。
     */
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

    /**
     * \u5e76\u53d1\u4e0b\u53e6\u4e00\u4e2a Reviewer \u5148\u62d2\u7edd\uff1a\u672c\u6b21\u901a\u8fc7\u5fc5\u987b\u5931\u8d25\uff0c\u6700\u7ec8\u53ea\u6709\u4e00\u4e2a\u7ec8\u6001。
     * 主要协作调用：<code>requestMapper.requestById</code>、<code>requestMapper.approveIfPending</code>。
     */
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

    /**
     * \u5ba1\u6838\u62d2\u7edd\uff1a\u539f\u56e0\u5fc5\u586b\uff0c\u7f3a\u5931\u65f6 REVIEW_REJECT_REASON_REQUIRED \u4e14\u4e0d\u66f4\u65b0\u6570\u636e\u5e93。
     * 主要协作调用：<code>requestMapper.requestById</code>。
     */
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

    /**
     * \u5ba1\u6838\u62d2\u7edd\u6210\u529f\uff1a\u62d2\u7edd\u539f\u56e0\u5199\u5165\u51b3\u5b9a\u4e0e\u52a8\u4f5c\u5386\u53f2\uff0c\u56de\u8c03\u5728\u63d0\u4ea4\u4e4b\u540e\u89e6\u53d1。
     * 主要协作调用：<code>requestMapper.requestById</code>、<code>requestMapper.rejectIfPending</code>。
     */
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

    /**
     * \u91cd\u590d\u62d2\u7edd\u540c\u4e00\u6761\u8bf7\u6c42\uff1aREVIEW_ALREADY_COMPLETED。
     * 主要协作调用：<code>requestMapper.requestById</code>、<code>requestMapper.rejectIfPending</code>。
     */
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

    /**
     * \u5ba1\u6838\u4e0d\u5b58\u5728\u7684\u8bf7\u6c42\uff1aREVIEW_NOT_FOUND。
     * 主要协作调用：<code>requestMapper.requestById</code>。
     */
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

    /**
     * \u51b3\u7b56\u4eba\u7f3a\u5931\uff1aREVIEW_ACCESS_DENIED\uff0c\u4e0d\u80fd\u51fa\u73b0\u6ca1\u6709\u8d23\u4efb\u4eba\u7684\u5ba1\u6838\u51b3\u5b9a。
     * 关键处理步骤调用 <code>service</code>、<code>service.approve</code>、<code>never</code>、<code>anyLong</code>。
     */
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

    /**
     * \u76ee\u6807\u6a21\u5757\u6ca1\u6709\u63a5\u5355\uff1a\u5ba1\u6838\u5728\u5199\u5e93\u4e4b\u524d\u5c31\u5931\u8d25\uff0c\u907f\u514d\u51fa\u73b0\u6ca1\u4eba\u5904\u7406\u7684 APPROVED。
     * 主要协作调用：<code>requestMapper.requestById</code>。
     */
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

    /**
     * \u6ca1\u6709\u6d3b\u52a8\u4e8b\u52a1\u65f6\u56de\u8c03\u7acb\u5373\u6267\u884c\uff1a\u56de\u8c03\u4e0d\u80fd\u88ab\u9759\u9ed8\u4e22\u5f03。
     * 主要协作调用：<code>requestMapper.requestById</code>、<code>requestMapper.approveIfPending</code>。
     */
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

    /**
     * \u6ce8\u518c\u8868\u4e25\u683c\u6309\u58f0\u660e\u7684 reviewType \u8def\u7531\uff1a\u65e0\u4eba\u8ba4\u9886\u62a5 NOT_SUPPORTED\uff0c\u91cd\u590d\u8ba4\u9886\u62a5 DECISION_CONFLICT。
     * 关键处理步骤调用 <code>List.of</code>、<code>single.supports</code>、<code>single.require</code>、<code>ambiguous.require</code>。
     */
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
