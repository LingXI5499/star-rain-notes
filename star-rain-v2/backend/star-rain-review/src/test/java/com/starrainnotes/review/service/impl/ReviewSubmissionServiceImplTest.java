package com.starrainnotes.review.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.review.api.ReviewTargetHandler;
import com.starrainnotes.review.api.ReviewTargetKey;
import com.starrainnotes.review.api.dto.ReviewSubmissionCommand;
import com.starrainnotes.review.api.dto.ReviewSubmissionResult;
import com.starrainnotes.review.entity.ReviewActionEntity;
import com.starrainnotes.review.entity.ReviewRequestEntity;
import com.starrainnotes.review.enumeration.ReviewActionType;
import com.starrainnotes.review.enumeration.ReviewActorType;
import com.starrainnotes.review.enumeration.ReviewStatus;
import com.starrainnotes.review.handler.impl.ReviewDemoTargetHandler;
import com.starrainnotes.review.handler.impl.ReviewTargetHandlerRegistryImpl;
import com.starrainnotes.review.mapper.ReviewActionMapper;
import com.starrainnotes.review.mapper.ReviewRequestMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/*
 * REV-001 提交审核 / REV-006 取消的单元测试。
 *
 * 重点覆盖三条最容易写错的不变量：
 *   1. 同一 target + reviewType 只能有一个 PENDING（含并发重复提交）；
 *   2. 提交必须写 SUBMITTED 动作，取消必须写 CANCELED 动作；
 *   3. 命名锁拿到就必须释放，拿不到则不能去释放别人的锁。
 *
 * 每个用例都用真实的 ReviewTargetHandlerRegistryImpl + ReviewDemoTargetHandler，
 * 而不是把注册表 mock 掉：否则「没有业务模块接单就拒绝提交」这条约束测不出来。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReviewSubmissionServiceImplTest {

    @Mock
    private ReviewRequestMapper requestMapper;

    @Mock
    private ReviewActionMapper actionMapper;

    // 这里刻意不用 @InjectMocks：注册表需要 List<ReviewTargetHandler>，
    // 用真实实现才能覆盖「没有 Handler 接单就拒绝提交」这条约束
    private ReviewSubmissionServiceImpl service() {
        return new ReviewSubmissionServiceImpl(requestMapper, actionMapper,
                new ReviewTargetHandlerRegistryImpl(List.of(new ReviewDemoTargetHandler())));
    }

    /*
     * Mockito 的 stub 会被同一测试类内后续用例继承，
     * 因此每个用例开始前显式重置：否则前一个用例「拿不到锁」的 stub 会让本用例
     * 在目标校验之前就以 REVIEW_ALREADY_PENDING 失败，掩盖真正的断言点。
     */
    @org.junit.jupiter.api.BeforeEach
    void resetMappers() {
        org.mockito.Mockito.reset(requestMapper, actionMapper);
    }

    private static ReviewSubmissionCommand command() {
        return ReviewSubmissionCommand.builder()
                .reviewType(ReviewDemoTargetHandler.REVIEW_TYPE)
                .targetModule(ReviewDemoTargetHandler.TARGET_MODULE)
                .targetType(ReviewDemoTargetHandler.TARGET_TYPE)
                .targetId(100L)
                .targetRevisionRef("revision:7")
                .targetDisplayName("《Java 程序设计》")
                .applicantAccountId(9L)
                .applicantDisplayName("账户 #9")
                .submissionNote("请审核第一章")
                .build();
    }

    private static ReviewTargetKey key() {
        return ReviewTargetKey.builder()
                .targetModule(ReviewDemoTargetHandler.TARGET_MODULE)
                .targetType(ReviewDemoTargetHandler.TARGET_TYPE)
                .targetId(100L)
                .build();
    }

    private static ReviewRequestEntity request(Long id, String status) {
        ReviewRequestEntity entity = new ReviewRequestEntity();
        entity.setId(id);
        entity.setReviewType(ReviewDemoTargetHandler.REVIEW_TYPE);
        entity.setTargetModule(ReviewDemoTargetHandler.TARGET_MODULE);
        entity.setTargetType(ReviewDemoTargetHandler.TARGET_TYPE);
        entity.setTargetId(100L);
        entity.setTargetRevisionRef("revision:7");
        entity.setTargetDisplayName("《Java 程序设计》");
        entity.setApplicantAccountId(9L);
        entity.setApplicantDisplayName("账户 #9");
        entity.setStatus(status);
        entity.setSubmittedAt(LocalDateTime.now());
        return entity;
    }

    @Test
    @DisplayName("提交成功：写入 PENDING 请求并同时写入 SUBMITTED 动作历史，最后释放命名锁")
    void submitCreatesPendingRequestAndSubmittedAction() {
        when(requestMapper.acquireTargetLock(anyString(), anyInt())).thenReturn(1);
        when(requestMapper.selectPending(any(), any(), any(), any())).thenReturn(null);
        // 插入后 MyBatis 回填自增主键，mock 里手工模拟
        when(requestMapper.insertRequest(any())).thenAnswer(invocation -> {
            ReviewRequestEntity entity = invocation.getArgument(0);
            entity.setId(500L);
            return 1;
        });
        when(requestMapper.requestById(500L)).thenReturn(request(500L, ReviewStatus.PENDING_CODE));

        ReviewSubmissionResult result = service().submit(command());

        assertThat(result.getReviewRequestId()).isEqualTo(500L);
        assertThat(result.getStatus()).isEqualTo(ReviewStatus.PENDING_CODE);
        assertThat(result.pending()).isTrue();
        assertThat(result.getTargetRevisionRef()).isEqualTo("revision:7");

        ArgumentCaptor<ReviewActionEntity> actionCaptor = ArgumentCaptor.forClass(ReviewActionEntity.class);
        verify(actionMapper).insertAction(actionCaptor.capture());
        assertThat(actionCaptor.getValue().getActionType()).isEqualTo(ReviewActionType.SUBMITTED_CODE);
        assertThat(actionCaptor.getValue().getActorAccountId()).isEqualTo(9L);
        assertThat(actionCaptor.getValue().getActorType()).isEqualTo(ReviewActorType.ACCOUNT_CODE);
        // 锁必须在方法返回前释放，否则同一目标后续提交全部超时
        verify(requestMapper).releaseTargetLock(anyString());
    }

    @Test
    @DisplayName("同一目标重复提交：selectPending 命中，返回 REVIEW_ALREADY_PENDING 且不插入")
    void submitRejectsDuplicatePending() {
        when(requestMapper.acquireTargetLock(anyString(), anyInt())).thenReturn(1);
        when(requestMapper.selectPending(eq(ReviewDemoTargetHandler.TARGET_MODULE),
                eq(ReviewDemoTargetHandler.TARGET_TYPE), eq(100L),
                eq(ReviewDemoTargetHandler.REVIEW_TYPE)))
                .thenReturn(request(500L, ReviewStatus.PENDING_CODE));

        assertThatThrownBy(() -> service().submit(command()))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("REVIEW_ALREADY_PENDING");

        verify(requestMapper, never()).insertRequest(any());
        verify(actionMapper, never()).insertAction(any());
        verify(requestMapper).releaseTargetLock(anyString());
    }

    @Test
    @DisplayName("并发重复提交：拿不到命名锁等同于已有待审，返回 REVIEW_ALREADY_PENDING 且不插入")
    void submitRejectsWhenLockNotAcquired() {
        when(requestMapper.acquireTargetLock(anyString(), anyInt())).thenReturn(0);

        assertThatThrownBy(() -> service().submit(command()))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("REVIEW_ALREADY_PENDING");

        verify(requestMapper, never()).insertRequest(any());
        // 没拿到锁就不该去释放别人的锁
        verify(requestMapper, never()).releaseTargetLock(anyString());
    }

    @Test
    @DisplayName("同一目标被重复提交两次：只有抢到锁的那一次写库，最终只有一条请求")
    void repeatedSubmitsOnlyOneInserts() {
        // 第一次拿到锁，第二次拿不到；用返回值序列模拟命名锁的串行化效果
        when(requestMapper.acquireTargetLock(anyString(), anyInt())).thenReturn(1, 0);
        when(requestMapper.selectPending(any(), any(), any(), any())).thenReturn(null);
        when(requestMapper.insertRequest(any())).thenAnswer(invocation -> {
            ReviewRequestEntity entity = invocation.getArgument(0);
            entity.setId(501L);
            return 1;
        });
        when(requestMapper.requestById(501L)).thenReturn(request(501L, ReviewStatus.PENDING_CODE));

        service().submit(command());
        assertThatThrownBy(() -> service().submit(command()))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("REVIEW_ALREADY_PENDING");

        verify(requestMapper, times(1)).insertRequest(any());
    }

    @Test
    @DisplayName("拒绝无人认领的 reviewType：显式声明后仍不匹配，返回 REVIEW_TARGET_NOT_SUPPORTED 且不落库")
    void submitRejectsUnsupportedTarget() {
        // 这个 Handler 明确只接单 demo.publish，因此 blog.publish 不应该被兜底接走
        ReviewTargetHandler onlyDemoPublish = new ReviewTargetHandler() {

            @Override
            public String targetModule() {
                return ReviewDemoTargetHandler.TARGET_MODULE;
            }

            @Override
            public String targetType() {
                return ReviewDemoTargetHandler.TARGET_TYPE;
            }

            @Override
            public boolean supports(String targetModule, String targetType, String reviewType) {
                return ReviewDemoTargetHandler.REVIEW_TYPE.equals(reviewType);
            }

            @Override
            public com.starrainnotes.review.api.ReviewTargetView loadReviewView(
                    com.starrainnotes.review.api.ReviewTargetRef target) {
                return null;
            }
        };
        ReviewSubmissionServiceImpl service = new ReviewSubmissionServiceImpl(requestMapper, actionMapper,
                new ReviewTargetHandlerRegistryImpl(List.of(onlyDemoPublish)));

        ReviewSubmissionCommand unknown = command();
        unknown.setReviewType("blog.publish");

        assertThatThrownBy(() -> service.submit(unknown))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("REVIEW_TARGET_NOT_SUPPORTED");

        verify(requestMapper, never()).insertRequest(any());
        verify(requestMapper, never()).acquireTargetLock(anyString(), anyInt());
    }

    @Test
    @DisplayName("同一 targetModule 下未被认领的 reviewType 也必须被拒，不做宽松兜底路由")
    void submitRejectsUnclaimedReviewTypeOfSameTarget() {
        when(requestMapper.acquireTargetLock(anyString(), anyInt())).thenReturn(1);
        when(requestMapper.selectPending(any(), any(), any(), any())).thenReturn(null);

        // 演示 Handler 的默认 supports 只比 module + type，所以 demo.republish 不在它的认领范围内
        ReviewSubmissionCommand otherType = command();
        otherType.setReviewType("demo.republish");

        assertThatThrownBy(() -> service().submit(otherType))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("REVIEW_TARGET_NOT_SUPPORTED");

        verify(requestMapper, never()).insertRequest(any());
    }

    @Test
    @DisplayName("缺少 targetRevisionRef 的命令被拒：没有冻结版本就没有可审核的对象")
    void submitRejectsMissingRevisionRef() {
        ReviewSubmissionCommand noRevision = command();
        noRevision.setTargetRevisionRef("  ");

        assertThatThrownBy(() -> service().submit(noRevision))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("REVIEW_COMMAND_INVALID");

        verify(requestMapper, never()).acquireTargetLock(anyString(), anyInt());
    }

    @Test
    @DisplayName("申请人取消自己的 PENDING：条件更新成功并写入 CANCELED 动作")
    void cancelByApplicantSucceeds() {
        when(requestMapper.requestById(500L)).thenReturn(request(500L, ReviewStatus.PENDING_CODE));
        when(requestMapper.cancelIfPending(eq(500L), any(LocalDateTime.class))).thenReturn(1);

        service().cancelByApplicant(500L, 9L);

        ArgumentCaptor<ReviewActionEntity> actionCaptor = ArgumentCaptor.forClass(ReviewActionEntity.class);
        verify(actionMapper).insertAction(actionCaptor.capture());
        assertThat(actionCaptor.getValue().getActionType()).isEqualTo(ReviewActionType.CANCELED_CODE);
        assertThat(actionCaptor.getValue().getActorAccountId()).isEqualTo(9L);
    }

    @Test
    @DisplayName("取消别人的 PENDING 被拒为 REVIEW_APPLICANT_MISMATCH，且不动数据库")
    void cancelByOtherAccountDenied() {
        when(requestMapper.requestById(500L)).thenReturn(request(500L, ReviewStatus.PENDING_CODE));

        assertThatThrownBy(() -> service().cancelByApplicant(500L, 77L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("REVIEW_APPLICANT_MISMATCH");

        verify(requestMapper, never()).cancelIfPending(any(), any());
    }

    @Test
    @DisplayName("已终态的请求不能再取消：REVIEW_NOT_PENDING")
    void cancelRejectedWhenTerminal() {
        when(requestMapper.requestById(500L)).thenReturn(request(500L, ReviewStatus.APPROVED_CODE));

        assertThatThrownBy(() -> service().cancelByApplicant(500L, 9L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("REVIEW_NOT_PENDING");

        verify(requestMapper, never()).cancelIfPending(any(), any());
    }

    @Test
    @DisplayName("并发下条件更新影响 0 行：取消必须报错，而不是静默成功")
    void cancelReportsRaceWhenConditionalUpdateMisses() {
        when(requestMapper.requestById(500L)).thenReturn(request(500L, ReviewStatus.PENDING_CODE));
        when(requestMapper.cancelIfPending(eq(500L), any(LocalDateTime.class))).thenReturn(0);

        assertThatThrownBy(() -> service().cancelByApplicant(500L, 9L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("REVIEW_NOT_PENDING");

        verify(actionMapper, never()).insertAction(any());
    }

    @Test
    @DisplayName("取消不存在的请求：REVIEW_NOT_FOUND")
    void cancelUnknownRequest() {
        when(requestMapper.requestById(404L)).thenReturn(null);

        assertThatThrownBy(() -> service().cancelByApplicant(404L, 9L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("REVIEW_NOT_FOUND");
    }

    @Test
    @DisplayName("系统取消是幂等的：找不到 PENDING 时静默返回，不报错也不写历史")
    void cancelBySystemIsIdempotent() {
        when(requestMapper.selectPending(any(), any(), any(), any())).thenReturn(null);

        service().cancelBySystem(key(), ReviewDemoTargetHandler.REVIEW_TYPE, "目标已删除");

        verify(requestMapper, never()).cancelIfPending(any(), any());
        verify(actionMapper, never()).insertAction(any());
    }

    @Test
    @DisplayName("系统取消写入 SYSTEM 类型的动作，actorAccountId 为空")
    void cancelBySystemWritesSystemActor() {
        when(requestMapper.selectPending(any(), any(), any(), any()))
                .thenReturn(request(500L, ReviewStatus.PENDING_CODE));
        when(requestMapper.cancelIfPending(eq(500L), any(LocalDateTime.class))).thenReturn(1);

        service().cancelBySystem(key(), ReviewDemoTargetHandler.REVIEW_TYPE, "目标已被删除");

        ArgumentCaptor<ReviewActionEntity> actionCaptor = ArgumentCaptor.forClass(ReviewActionEntity.class);
        verify(actionMapper).insertAction(actionCaptor.capture());
        assertThat(actionCaptor.getValue().getActorType()).isEqualTo(ReviewActorType.SYSTEM_CODE);
        assertThat(actionCaptor.getValue().getActorAccountId()).isNull();
        assertThat(actionCaptor.getValue().getNote()).isEqualTo("目标已被删除");
    }

    @Test
    @DisplayName("findActivePending：有待审返回摘要，target 为空返回空 Optional")
    void findActivePendingBranches() {
        when(requestMapper.selectPending(any(), any(), any(), any()))
                .thenReturn(request(500L, ReviewStatus.PENDING_CODE));

        assertThat(service().findActivePending(key(), ReviewDemoTargetHandler.REVIEW_TYPE))
                .isPresent()
                .get()
                .extracting(summary -> summary.getStatus())
                .isEqualTo(ReviewStatus.PENDING_CODE);
        assertThat(service().findActivePending(null, ReviewDemoTargetHandler.REVIEW_TYPE)).isEmpty();
    }
}
