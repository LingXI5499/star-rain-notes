/*
 * ReviewDecisionServiceImpl：审核模块的业务实现文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：ReviewDecisionContext、ReviewTargetHandler、ReviewTargetRef、ReviewApproveCommand、ReviewRejectCommand、ReviewActionEntity、ReviewRequestEntity。
 * 阅读入口：approve（批准审核决定）；reject（拒绝审核决定）；recoverApproved（recover已批准处理入口）。
 */

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
/**
 * 审核决定业务服务实现，属于审核模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
public class ReviewDecisionServiceImpl implements ReviewDecisionService {

    /**
     * 请求数据映射。
     * 协作类型：<code>ReviewRequestMapper</code>。
     */
    private final ReviewRequestMapper requestMapper;
    /**
     * 动作数据映射。
     * 协作类型：<code>ReviewActionMapper</code>。
     */
    private final ReviewActionMapper actionMapper;
    /**
     * 处理器Registry。
     */
    private final ReviewTargetHandlerRegistry handlerRegistry;
    /**
     * callbackDispatcher。
     */
    private final ReviewDecisionCallbackDispatcher callbackDispatcher;

    /**
     * 构造ReviewDecisionServiceImpl，接收本类使用的依赖或初始化参数。
     *
     * @param requestMapper 请求数据映射；类型为 <code>ReviewRequestMapper</code>
     * @param actionMapper 动作数据映射；类型为 <code>ReviewActionMapper</code>
     * @param handlerRegistry 处理器Registry；类型为 <code>ReviewTargetHandlerRegistry</code>
     * @param callbackDispatcher callbackDispatcher；类型为 <code>ReviewDecisionCallbackDispatcher</code>
     */
    public ReviewDecisionServiceImpl(ReviewRequestMapper requestMapper,
                                     ReviewActionMapper actionMapper,
                                     ReviewTargetHandlerRegistry handlerRegistry,
                                     ReviewDecisionCallbackDispatcher callbackDispatcher) {
        this.requestMapper = requestMapper;
        this.actionMapper = actionMapper;
        this.handlerRegistry = handlerRegistry;
        this.callbackDispatcher = callbackDispatcher;
    }

    /**
     * 批准审核决定。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>handlerRegistry.require</code>、<code>requestMapper.approveIfPending</code>、<code>actionMapper.insertAction</code>、<code>callbackDispatcher.dispatch</code>。
     * 实现中的显式失败类型包括 <code>ReviewAlreadyCompletedException</code>；具体触发条件见对应分支。
     *
     * @param reviewRequestId 审核请求标识；类型为 <code>Long</code>
     * @param command command；类型为 <code>ReviewApproveCommand</code>
     * @param reviewerAccountId 审核人账号标识；类型为 <code>Long</code>
     */
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

    /**
     * 拒绝审核决定。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>handlerRegistry.require</code>、<code>requestMapper.rejectIfPending</code>、<code>actionMapper.insertAction</code>、<code>callbackDispatcher.dispatch</code>。
     * 实现中的显式失败类型包括 <code>ReviewRejectReasonRequiredException</code>、<code>ReviewAlreadyCompletedException</code>；具体触发条件见对应分支。
     *
     * @param reviewRequestId 审核请求标识；类型为 <code>Long</code>
     * @param command command；类型为 <code>ReviewRejectCommand</code>
     * @param reviewerAccountId 审核人账号标识；类型为 <code>Long</code>
     */
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

    /**
     * 检查所需请求。
     * 主要协作调用：<code>requestMapper.requestById</code>。
     * 实现中的显式失败类型包括 <code>ReviewNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param reviewRequestId 审核请求标识；类型为 <code>Long</code>
     * @return 类型为 <code>ReviewRequestEntity</code> 的处理结果
     */
    private ReviewRequestEntity requireRequest(Long reviewRequestId) {
        ReviewRequestEntity request = reviewRequestId == null ? null : requestMapper.requestById(reviewRequestId);
        if (request == null) {
            throw new ReviewNotFoundException();
        }
        return request;
    }

    /**
     * recover已批准处理入口。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 调用约束：<code>@org.springframework.security.access.prepost.PreAuthorize("hasRole(\'SUPER_ADMIN\') and hasAuthority(\'review:approve\')")</code>。
     * 主要协作调用：<code>handlerRegistry.require</code>、<code>actionMapper.insertAction</code>、<code>callbackDispatcher.dispatch</code>。
     * 实现中的显式失败类型包括 <code>ReviewAlreadyCompletedException</code>、<code>ReviewAccessDeniedException</code>；具体触发条件见对应分支。
     *
     * @param reviewRequestId 审核请求标识；类型为 <code>Long</code>
     * @param reviewerAccountId 审核人账号标识；类型为 <code>Long</code>
     */
    @Override
    @Transactional
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('review:approve')")
    public void recoverApproved(Long reviewRequestId, Long reviewerAccountId) {
        requireReviewer(reviewerAccountId);
        ReviewRequestEntity request = requireRequest(reviewRequestId);
        if (!ReviewStatus.APPROVED_CODE.equals(request.getStatus())) throw new ReviewAlreadyCompletedException();
        ReviewTargetHandler handler = handlerRegistry.require(request.getTargetModule(), request.getTargetType(), request.getReviewType());
        if (!handler.supportsApprovedRecovery()) throw new ReviewAccessDeniedException();
        LocalDateTime attemptedAt = LocalDateTime.now();
        actionMapper.insertAction(action(request.getId(), "RECOVERY_REQUESTED", reviewerAccountId,
                ReviewActorType.ACCOUNT_CODE, "显式重新核实并恢复业务应用；审批结论保持不变", attemptedAt));
        ReviewDecisionContext context = context(request, ReviewActionType.RECOVERY_REQUESTED, ReviewStatus.APPROVED_CODE,
                request.getDecisionReason(), reviewerAccountId, request.getDecidedAt());
        callbackDispatcher.dispatch(() -> handler.recoverApproved(context));
    }

    // 审核动作必须有人负责：决策人缺失属于调用方越权或内部错误，不能容忍
    /**
     * 检查所需审核人。
     * 实现中的显式失败类型包括 <code>ReviewAccessDeniedException</code>；具体触发条件见对应分支。
     *
     * @param reviewerAccountId 审核人账号标识；类型为 <code>Long</code>
     */
    private static void requireReviewer(Long reviewerAccountId) {
        if (reviewerAccountId == null) {
            throw new ReviewAccessDeniedException();
        }
    }

    /**
     * context处理入口。
     * 关键处理步骤调用 <code>ReviewDecisionContext.builder</code>、<code>request.getId</code>、<code>request.getReviewType</code>、<code>ReviewTargetRef.builder</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>ReviewRequestEntity</code>
     * @param actionType 动作类型；类型为 <code>ReviewActionType</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>
     * @param reason 原因；类型为 <code>String</code>
     * @param reviewerAccountId 审核人账号标识；类型为 <code>Long</code>
     * @param decidedAt 决定时间；类型为 <code>LocalDateTime</code>
     * @return 类型为 <code>ReviewDecisionContext</code> 的处理结果
     */
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

    /**
     * 动作处理入口。
     * 关键处理步骤调用 <code>action.setReviewRequestId</code>、<code>action.setActionType</code>、<code>action.setActorAccountId</code>、<code>action.setActorType</code>。
     *
     * @param reviewRequestId 审核请求标识；类型为 <code>Long</code>
     * @param actionType 动作类型；类型为 <code>String</code>
     * @param actorAccountId 操作者账号标识；类型为 <code>Long</code>
     * @param actorType 操作者类型；类型为 <code>String</code>
     * @param note note；类型为 <code>String</code>
     * @param createdAt 创建时间；类型为 <code>LocalDateTime</code>
     * @return 类型为 <code>ReviewActionEntity</code> 的处理结果
     */
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
