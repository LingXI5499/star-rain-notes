package com.starrainnotes.review.service.impl;

import com.starrainnotes.review.api.ReviewSummary;
import com.starrainnotes.review.api.ReviewTargetKey;
import com.starrainnotes.review.api.dto.ReviewSubmissionCommand;
import com.starrainnotes.review.api.dto.ReviewSubmissionResult;
import com.starrainnotes.review.entity.ReviewActionEntity;
import com.starrainnotes.review.entity.ReviewRequestEntity;
import com.starrainnotes.review.enumeration.ReviewActionType;
import com.starrainnotes.review.enumeration.ReviewActorType;
import com.starrainnotes.review.enumeration.ReviewStatus;
import com.starrainnotes.review.exception.ReviewAlreadyPendingException;
import com.starrainnotes.review.exception.ReviewApplicantMismatchException;
import com.starrainnotes.review.exception.ReviewCommandInvalidException;
import com.starrainnotes.review.exception.ReviewDecisionConflictException;
import com.starrainnotes.review.exception.ReviewNotFoundException;
import com.starrainnotes.review.exception.ReviewNotPendingException;
import com.starrainnotes.review.handler.ReviewTargetHandlerRegistry;
import com.starrainnotes.review.mapper.ReviewActionMapper;
import com.starrainnotes.review.mapper.ReviewRequestMapper;
import com.starrainnotes.review.service.ReviewSubmissionService;
import com.starrainnotes.review.utils.ReviewTargetKeys;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * REV-001 提交审核 / REV-006 取消待审请求，同时作为 ReviewSubmissionApi 的实现。
 *
 * 「同一 target + reviewType 只允许一个 PENDING」的三层保证：
 *   1. 先取 MySQL 命名锁（按 targetModule+targetType+targetId+reviewType 哈希），
 *      把「检查 + 插入」变成一个临界区，这是并发重复提交的主要防线；
 *   2. 锁内再查一次 selectPending，命中即抛 REVIEW_ALREADY_PENDING；
 *   3. 普通重复提交（非并发）同样被第 2 步拦住。
 *
 * 为什么不用唯一索引：MySQL 没有「只对 PENDING 唯一」的条件唯一索引，
 * 生成列方案会引入业务无关的列，规范也明确当前阶段由 Service 保证。
 *
 * 事务边界：submit 必须运行在事务里，否则命名锁会在每条语句后随连接归还而失效，
 * 临界区不成立（activeTransactionRequired 就是为这个约束把关）。
 */
@Service
public class ReviewSubmissionServiceImpl implements ReviewSubmissionService {

    // 命名锁等待上限：提交是用户操作，等太久不如直接告诉他稍后重试
    private static final int LOCK_TIMEOUT_SECONDS = 5;

    private final ReviewRequestMapper requestMapper;
    private final ReviewActionMapper actionMapper;
    private final ReviewTargetHandlerRegistry handlerRegistry;

    public ReviewSubmissionServiceImpl(ReviewRequestMapper requestMapper,
                                       ReviewActionMapper actionMapper,
                                       ReviewTargetHandlerRegistry handlerRegistry) {
        this.requestMapper = requestMapper;
        this.actionMapper = actionMapper;
        this.handlerRegistry = handlerRegistry;
    }

    @Override
    @Transactional
    public ReviewSubmissionResult submit(ReviewSubmissionCommand command) {
        requireCommand(command);
        // 没有业务模块接单就拒绝：提交一个谁都不认识的 reviewType 属于接入错误
        handlerRegistry.require(command.getTargetModule(), command.getTargetType(), command.getReviewType());

        ReviewTargetKey target = ReviewTargetKey.builder()
                .targetModule(command.getTargetModule())
                .targetType(command.getTargetType())
                .targetId(command.getTargetId())
                .build();
        String lockName = ReviewTargetKeys.lockName(target, command.getReviewType());
        acquireLock(lockName);
        try {
            if (requestMapper.selectPending(command.getTargetModule(), command.getTargetType(),
                    command.getTargetId(), command.getReviewType()) != null) {
                throw new ReviewAlreadyPendingException();
            }
            LocalDateTime now = LocalDateTime.now();
            ReviewRequestEntity entity = new ReviewRequestEntity();
            entity.setReviewType(command.getReviewType());
            entity.setTargetModule(command.getTargetModule());
            entity.setTargetType(command.getTargetType());
            entity.setTargetId(command.getTargetId());
            entity.setTargetRevisionRef(command.getTargetRevisionRef());
            entity.setTargetDisplayName(command.getTargetDisplayName());
            entity.setApplicantAccountId(command.getApplicantAccountId());
            entity.setApplicantDisplayName(command.getApplicantDisplayName());
            entity.setSubmissionNote(command.getSubmissionNote());
            entity.setStatus(ReviewStatus.PENDING_CODE);
            entity.setSubmittedAt(now);
            requestMapper.insertRequest(entity);

            if (requestMapper.requestById(entity.getId()) == null) {
                // 理论不可达：插入成功却读不到说明连接或映射出了问题，宁可失败也不要留下无轨迹的请求
                throw new ReviewDecisionConflictException();
            }
            // 提交动作也写历史，保证轨迹永远从 SUBMITTED 开始
            actionMapper.insertAction(action(entity.getId(), ReviewActionType.SUBMITTED_CODE,
                    command.getApplicantAccountId(), ReviewActorType.ACCOUNT_CODE,
                    command.getSubmissionNote(), now));

            return ReviewSubmissionResult.builder()
                    .reviewRequestId(entity.getId())
                    .status(ReviewStatus.PENDING_CODE)
                    .reviewType(entity.getReviewType())
                    .targetModule(entity.getTargetModule())
                    .targetType(entity.getTargetType())
                    .targetId(entity.getTargetId())
                    .targetRevisionRef(entity.getTargetRevisionRef())
                    .submittedAt(now)
                    .build();
        } finally {
            // 必须在事务提交前释放：命名锁与环境里的连接绑定，
            // 而 Spring 可能在本方法返回后（仍持有连接时）才提交事务
            releaseLock(lockName);
        }
    }

    @Override
    @Transactional
    public void cancelByApplicant(Long reviewRequestId, Long actorAccountId) {
        ReviewRequestEntity request = requireRequest(reviewRequestId);
        if (!ReviewStatus.PENDING_CODE.equals(request.getStatus())) {
            // 已经是终态：申请人多半是刷新不及时，给出明确错误码而不是静默成功
            throw new ReviewNotPendingException();
        }
        if (!request.getApplicantAccountId().equals(actorAccountId)) {
            throw new ReviewApplicantMismatchException();
        }
        cancel(request, actorAccountId, ReviewActorType.ACCOUNT_CODE, "申请人取消待审核请求");
    }

    @Override
    @Transactional
    public void cancelBySystem(ReviewTargetKey target, String reviewType, String reason) {
        if (target == null) {
            return;
        }
        // 系统动作找的是「某个目标当前有没有待审」，而不是某条具体请求
        ReviewRequestEntity pending = requestMapper.selectPending(target.getTargetModule(),
                target.getTargetType(), target.getTargetId(), reviewType);
        if (pending == null) {
            // 目标已经没有待审请求时系统取消是幂等的：可能刚被别人审完或取消
            return;
        }
        cancel(pending, null, ReviewActorType.SYSTEM_CODE,
                reason == null || reason.isBlank() ? "目标已变更，系统自动取消待审请求" : reason);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReviewSummary> findActivePending(ReviewTargetKey target, String reviewType) {
        if (target == null) {
            return Optional.empty();
        }
        ReviewRequestEntity pending = requestMapper.selectPending(target.getTargetModule(),
                target.getTargetType(), target.getTargetId(), reviewType);
        return Optional.ofNullable(pending).map(this::summary);
    }

    // 取消的共同路径：条件更新 + 写历史，条件更新影响 0 行说明并发中已被处理
    private void cancel(ReviewRequestEntity request, Long actorAccountId, String actorType, String note) {
        LocalDateTime now = LocalDateTime.now();
        int updated = requestMapper.cancelIfPending(request.getId(), now);
        if (updated != 1) {
            // 事务内已确认是 PENDING 却更新不到，只可能是并发决策抢先，
            // 这与「已被处理」语义一致，不需要单独的冲突码
            throw new ReviewNotPendingException();
        }
        actionMapper.insertAction(action(request.getId(), ReviewActionType.CANCELED_CODE,
                actorAccountId, actorType, note, now));
    }

    private ReviewRequestEntity requireRequest(Long reviewRequestId) {
        ReviewRequestEntity request = reviewRequestId == null ? null : requestMapper.requestById(reviewRequestId);
        if (request == null) {
            throw new ReviewNotFoundException();
        }
        return request;
    }

    private void acquireLock(String lockName) {
        Integer acquired = requestMapper.acquireTargetLock(lockName, LOCK_TIMEOUT_SECONDS);
        if (acquired == null || acquired != 1) {
            // 拿不到锁说明同一目标正有另一次提交在临界区里，
            // 结果一定是「已经有 PENDING」或马上会有，直接按重复提交处理
            throw new ReviewAlreadyPendingException();
        }
    }

    private void releaseLock(String lockName) {
        try {
            requestMapper.releaseTargetLock(lockName);
        } catch (RuntimeException ignored) {
            // 释放失败不能影响已经成功的提交：连接回收时 MySQL 会自行释放命名锁
        }
    }

    private ReviewSummary summary(ReviewRequestEntity entity) {
        return ReviewSummary.builder()
                .reviewRequestId(entity.getId())
                .reviewType(entity.getReviewType())
                .targetModule(entity.getTargetModule())
                .targetType(entity.getTargetType())
                .targetId(entity.getTargetId())
                .targetRevisionRef(entity.getTargetRevisionRef())
                .status(entity.getStatus())
                .applicantAccountId(entity.getApplicantAccountId())
                .submittedAt(entity.getSubmittedAt())
                .decidedAt(entity.getDecidedAt())
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

    // 内部调用方（业务模块）也可能构造出不完整命令，这里做最小必要校验
    private static void requireCommand(ReviewSubmissionCommand command) {
        if (command == null) {
            throw new ReviewCommandInvalidException("提交审核的命令不能为空");
        }
        if (command.getApplicantAccountId() == null) {
            throw new ReviewCommandInvalidException("提交审核必须携带申请人账户ID");
        }
        if (command.getTargetId() == null) {
            throw new ReviewCommandInvalidException("提交审核必须携带目标对象ID");
        }
        if (isBlank(command.getReviewType())) {
            throw new ReviewCommandInvalidException("提交审核必须携带 reviewType");
        }
        if (isBlank(command.getTargetModule()) || isBlank(command.getTargetType())) {
            throw new ReviewCommandInvalidException("提交审核必须携带 targetModule 与 targetType");
        }
        if (isBlank(command.getTargetRevisionRef())) {
            // 这条最关键：没有冻结版本就没法保证 Reviewer 审的是提交时那一版
            throw new ReviewCommandInvalidException("提交审核必须携带不可变的 targetRevisionRef");
        }
        if (isBlank(command.getTargetDisplayName())) {
            throw new ReviewCommandInvalidException("提交审核必须携带 targetDisplayName 快照");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
