package com.starrainnotes.review.service.impl;

import com.starrainnotes.review.api.ReviewDecisionContext;
import com.starrainnotes.review.api.ReviewTargetHandler;
import com.starrainnotes.review.api.ReviewTargetRef;
import com.starrainnotes.review.dto.ReviewApproveCommand;
import com.starrainnotes.review.dto.ReviewRejectCommand;
import com.starrainnotes.review.entity.ReviewActionEntity;
import com.starrainnotes.review.entity.ReviewRequestEntity;
import com.starrainnotes.review.enumeration.ReviewActionType;
import com.starrainnotes.review.enumeration.ReviewActorType;
import com.starrainnotes.review.enumeration.ReviewStatus;
import com.starrainnotes.review.exception.ReviewAccessDeniedException;
import com.starrainnotes.review.exception.ReviewAlreadyCompletedException;
import com.starrainnotes.review.exception.ReviewNotFoundException;
import com.starrainnotes.review.exception.ReviewRejectReasonRequiredException;
import com.starrainnotes.review.handler.ReviewDecisionCallbackDispatcher;
import com.starrainnotes.review.handler.ReviewTargetHandlerRegistry;
import com.starrainnotes.review.mapper.ReviewActionMapper;
import com.starrainnotes.review.mapper.ReviewRequestMapper;
import com.starrainnotes.review.service.ReviewDecisionService;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * REV-004 审核通过 / REV-005 审核拒绝。
 *
 * 并发策略（规范第 10 节）：
 *   禁止「select 状态 → if pending → 普通 update」，那样两个 Reviewer 会互相覆盖。
 *   这里一律用带 status='PENDING' 条件的 UPDATE，affected_rows == 1 才算本次决定生效；
 *   影响 0 行说明已经被别人处理，直接抛 REVIEW_ALREADY_COMPLETED。
 *   同一事务里先写条件更新再写 ReviewAction，因此「状态变了但历史没写」不可能出现。
 *
 * 回调时机（本模块最关键的架构约束）：
 *   目标业务模块的状态推进不在这里做，而是把 onApproved / onRejected 注册成
 *   afterCommit 回调。审核决定先提交，业务模块随后推进自己的状态；
 *   业务模块派生动作失败不会把已经生效的审核决定回滚掉。
 *   Review 全程不注入任何业务 Mapper，也不读业务表。
 */
@Service
public class ReviewDecisionServiceImpl implements ReviewDecisionService {

    private final ReviewRequestMapper requestMapper;
    private final ReviewActionMapper actionMapper;
    private final ReviewTargetHandlerRegistry handlerRegistry;
    private final ReviewDecisionCallbackDispatcher callbackDispatcher;

    public ReviewDecisionServiceImpl(ReviewRequestMapper requestMapper,
                                     ReviewActionMapper actionMapper,
                                     ReviewTargetHandlerRegistry handlerRegistry,
                                     ReviewDecisionCallbackDispatcher callbackDispatcher) {
        this.requestMapper = requestMapper;
        this.actionMapper = actionMapper;
        this.handlerRegistry = handlerRegistry;
        this.callbackDispatcher = callbackDispatcher;
    }

    @Override
    @Transactional
    public void approve(Long reviewRequestId, ReviewApproveCommand command, Long reviewerAccountId) {
        requireReviewer(reviewerAccountId);
        ReviewRequestEntity request = requireRequest(reviewRequestId);
        // 提前解析 Handler：如果目标模块根本没接入（或已下线），
        // 要在写库之前失败，而不是留下一条「通过了但没人处理」的请求
        ReviewTargetHandler handler = handlerRegistry.require(request.getTargetModule(),
                request.getTargetType(), request.getReviewType());

        String note = command == null ? null : command.getNote();
        LocalDateTime decidedAt = LocalDateTime.now();
        int updated = requestMapper.approveIfPending(request.getId(), reviewerAccountId, note, decidedAt);
        if (updated != 1) {
            throw new ReviewAlreadyCompletedException();
        }
        actionMapper.insertAction(action(request.getId(), ReviewActionType.APPROVED_CODE,
                reviewerAccountId, ReviewActorType.ACCOUNT_CODE, note, decidedAt));

        ReviewDecisionContext context = context(request, ReviewActionType.APPROVED,
                ReviewStatus.APPROVED_CODE, note, reviewerAccountId, decidedAt);
        callbackDispatcher.dispatch(() -> handler.onApproved(context));
    }

    @Override
    @Transactional
    public void reject(Long reviewRequestId, ReviewRejectCommand command, Long reviewerAccountId) {
        requireReviewer(reviewerAccountId);
        // 拒绝原因必填：它是申请人修改后重新提交的唯一依据
        if (command == null || command.getReason() == null || command.getReason().isBlank()) {
            throw new ReviewRejectReasonRequiredException();
        }
        ReviewRequestEntity request = requireRequest(reviewRequestId);
        ReviewTargetHandler handler = handlerRegistry.require(request.getTargetModule(),
                request.getTargetType(), request.getReviewType());

        String reason = command.getReason().trim();
        LocalDateTime decidedAt = LocalDateTime.now();
        int updated = requestMapper.rejectIfPending(request.getId(), reviewerAccountId, reason, decidedAt);
        if (updated != 1) {
            throw new ReviewAlreadyCompletedException();
        }
        actionMapper.insertAction(action(request.getId(), ReviewActionType.REJECTED_CODE,
                reviewerAccountId, ReviewActorType.ACCOUNT_CODE, reason, decidedAt));

        ReviewDecisionContext context = context(request, ReviewActionType.REJECTED,
                ReviewStatus.REJECTED_CODE, reason, reviewerAccountId, decidedAt);
        callbackDispatcher.dispatch(() -> handler.onRejected(context));
    }

    private ReviewRequestEntity requireRequest(Long reviewRequestId) {
        ReviewRequestEntity request = reviewRequestId == null ? null : requestMapper.requestById(reviewRequestId);
        if (request == null) {
            throw new ReviewNotFoundException();
        }
        return request;
    }

    // 审核动作必须有人负责：决策人缺失属于调用方越权或内部错误，不能容忍
    private static void requireReviewer(Long reviewerAccountId) {
        if (reviewerAccountId == null) {
            throw new ReviewAccessDeniedException();
        }
    }

    private static ReviewDecisionContext context(ReviewRequestEntity request, ReviewActionType actionType,
                                                 String status, String reason, Long reviewerAccountId,
                                                 LocalDateTime decidedAt) {
        return ReviewDecisionContext.builder()
                .reviewRequestId(request.getId())
                .reviewType(request.getReviewType())
                .target(ReviewTargetRef.builder()
                        .targetModule(request.getTargetModule())
                        .targetType(request.getTargetType())
                        .targetId(request.getTargetId())
                        .revisionRef(request.getTargetRevisionRef())
                        .build())
                .status(status)
                .actionType(actionType)
                .reason(reason)
                .reviewerAccountId(reviewerAccountId)
                .applicantAccountId(request.getApplicantAccountId())
                .targetDisplayName(request.getTargetDisplayName())
                .decidedAt(decidedAt)
                .build();
    }

    private static ReviewActionEntity action(Long reviewRequestId, String actionType,
                                             Long actorAccountId, String actorType,
                                             String note, LocalDateTime createdAt) {
        ReviewActionEntity action = new ReviewActionEntity();
        action.setReviewRequestId(reviewRequestId);
        action.setActionType(actionType);
        action.setActorAccountId(actorAccountId);
        action.setActorType(actorType);
        action.setNote(note);
        action.setCreatedAt(createdAt);
        return action;
    }
}
