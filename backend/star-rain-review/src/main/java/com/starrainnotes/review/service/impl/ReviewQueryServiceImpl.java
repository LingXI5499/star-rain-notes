package com.starrainnotes.review.service.impl;

import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.review.api.ReviewTargetRef;
import com.starrainnotes.review.api.ReviewTargetView;
import com.starrainnotes.review.constant.ReviewPermissions;
import com.starrainnotes.review.context.ReviewViewer;
import com.starrainnotes.review.dto.ReviewHistoryQueryDTO;
import com.starrainnotes.review.dto.ReviewQueryDTO;
import com.starrainnotes.review.entity.ReviewActionEntity;
import com.starrainnotes.review.entity.ReviewRequestEntity;
import com.starrainnotes.review.enumeration.ReviewActionType;
import com.starrainnotes.review.enumeration.ReviewActorType;
import com.starrainnotes.review.enumeration.ReviewStatus;
import com.starrainnotes.review.exception.ReviewAccessDeniedException;
import com.starrainnotes.review.exception.ReviewNotFoundException;
import com.starrainnotes.review.exception.ReviewQueryInvalidException;
import com.starrainnotes.review.exception.ReviewTargetRevisionNotFoundException;
import com.starrainnotes.review.handler.ReviewTargetHandlerRegistry;
import com.starrainnotes.review.mapper.ReviewActionMapper;
import com.starrainnotes.review.mapper.ReviewRequestMapper;
import com.starrainnotes.review.service.ReviewQueryService;
import com.starrainnotes.review.vo.ReviewActionVO;
import com.starrainnotes.review.vo.ReviewDetailVO;
import com.starrainnotes.review.vo.ReviewHistoryVO;
import com.starrainnotes.review.vo.ReviewListItemVO;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * REV-002 待审核列表 / REV-003 审核详情 / REV-007 审核历史。
 *
 * 三个查询的共同约束：
 *   1. 分页参数越界与非法筛选值一律抛 REVIEW_QUERY_INVALID，不静默纠正；
 *   2. 列表与历史只用提交时冻结的显示名快照，绝不跨模块查标题；
 *   3. 详情里的目标正文一律走 TargetHandler.loadReviewView(revisionRef)，
 *      保证 Reviewer 看到的是提交时那一版，而不是目标对象的当前数据。
 *
 * REV-003 的对象级授权在这里做，而不是只靠 URL 层的 authenticated()：
 * 满足「actor 就是申请人」或「actor 拥有 review:read」之一才放行，
 * 否则返回 REVIEW_ACCESS_DENIED，避免 IDOR。
 */
@Service
public class ReviewQueryServiceImpl implements ReviewQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ReviewRequestMapper requestMapper;
    private final ReviewActionMapper actionMapper;
    private final ReviewTargetHandlerRegistry handlerRegistry;

    public ReviewQueryServiceImpl(ReviewRequestMapper requestMapper,
                                  ReviewActionMapper actionMapper,
                                  ReviewTargetHandlerRegistry handlerRegistry) {
        this.requestMapper = requestMapper;
        this.actionMapper = actionMapper;
        this.handlerRegistry = handlerRegistry;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ReviewListItemVO> pagePending(ReviewQueryDTO query) {
        ReviewQueryDTO condition = query == null ? new ReviewQueryDTO() : query;
        int page = normalizePage(condition.getPage());
        int pageSize = normalizePageSize(condition.getPageSize());
        String targetModule = trimToNull(condition.getTargetModule());
        String reviewType = trimToNull(condition.getReviewType());
        String keyword = trimToNull(condition.getKeyword());

        long total = requestMapper.pendingCount(targetModule, reviewType, keyword);
        List<ReviewListItemVO> items = requestMapper
                .pendingPage(targetModule, reviewType, keyword, (page - 1) * pageSize, pageSize)
                .stream()
                .map(ReviewQueryServiceImpl::toListItem)
                .toList();
        return new PageResult<>(items, total, page, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewDetailVO getDetail(Long reviewRequestId, ReviewViewer viewer) {
        ReviewRequestEntity request = reviewRequestId == null ? null : requestMapper.requestById(reviewRequestId);
        if (request == null) {
            throw new ReviewNotFoundException();
        }
        if (!canView(request, viewer)) {
            throw new ReviewAccessDeniedException();
        }

        List<ReviewActionVO> history = actionMapper.selectByReviewRequestId(request.getId())
                .stream()
                .map(action -> toActionVO(action, request))
                .toList();

        return ReviewDetailVO.builder()
                .reviewId(request.getId())
                .reviewType(request.getReviewType())
                .targetModule(request.getTargetModule())
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .targetRevisionRef(request.getTargetRevisionRef())
                .targetDisplayName(request.getTargetDisplayName())
                .applicantAccountId(request.getApplicantAccountId())
                .applicantDisplayName(request.getApplicantDisplayName())
                .submissionNote(request.getSubmissionNote())
                .status(request.getStatus())
                .reviewerAccountId(request.getReviewerAccountId())
                .decisionReason(request.getDecisionReason())
                .submittedAt(request.getSubmittedAt())
                .decidedAt(request.getDecidedAt())
                .canceledAt(request.getCanceledAt())
                .targetView(loadTargetView(request))
                .history(history)
                .canApprove(canDecide(viewer, request, ReviewPermissions.APPROVE))
                .canReject(canDecide(viewer, request, ReviewPermissions.REJECT))
                .canCancel(canCancel(viewer, request))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ReviewHistoryVO> pageHistory(ReviewHistoryQueryDTO query) {
        ReviewHistoryQueryDTO condition = query == null ? new ReviewHistoryQueryDTO() : query;
        int page = normalizePage(condition.getPage());
        int pageSize = normalizePageSize(condition.getPageSize());
        String status = normalizeStatus(condition.getStatus());
        if (condition.getStartTime() != null && condition.getEndTime() != null
                && condition.getStartTime().isAfter(condition.getEndTime())) {
            throw new ReviewQueryInvalidException("startTime 不能晚于 endTime");
        }

        String targetModule = trimToNull(condition.getTargetModule());
        String targetType = trimToNull(condition.getTargetType());
        String reviewType = trimToNull(condition.getReviewType());

        long total = requestMapper.historyCount(targetModule, targetType, condition.getTargetId(),
                reviewType, status, condition.getApplicantAccountId(), condition.getReviewerAccountId(),
                condition.getStartTime(), condition.getEndTime());
        List<ReviewHistoryVO> items = requestMapper
                .historyPage(targetModule, targetType, condition.getTargetId(), reviewType, status,
                        condition.getApplicantAccountId(), condition.getReviewerAccountId(),
                        condition.getStartTime(), condition.getEndTime(),
                        (page - 1) * pageSize, pageSize)
                .stream()
                .map(ReviewQueryServiceImpl::toHistory)
                .toList();
        return new PageResult<>(items, total, page, pageSize);
    }

    // ---------- 授权判定 ----------

    // 申请人本人或具备 review:read 的后台都可以看详情
    private static boolean canView(ReviewRequestEntity request, ReviewViewer viewer) {
        if (viewer == null) {
            return false;
        }
        return viewer.isApplicant(request.getApplicantAccountId()) || viewer.reviewer();
    }

    // 只有 PENDING 才谈得上「可以决定」，终态一律不给按钮
    private static boolean canDecide(ReviewViewer viewer, ReviewRequestEntity request, String permission) {
        return viewer != null
                && ReviewStatus.PENDING_CODE.equals(request.getStatus())
                && viewer.hasPermission(permission);
    }

    // 取消只属于申请人本人：Reviewer 不应该替申请人撤回请求
    private static boolean canCancel(ReviewViewer viewer, ReviewRequestEntity request) {
        return viewer != null
                && ReviewStatus.PENDING_CODE.equals(request.getStatus())
                && viewer.isApplicant(request.getApplicantAccountId());
    }

    // ---------- 目标视图 ----------

    private ReviewTargetView loadTargetView(ReviewRequestEntity request) {
        ReviewTargetRef target = ReviewTargetRef.builder()
                .targetModule(request.getTargetModule())
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .revisionRef(request.getTargetRevisionRef())
                .build();
        ReviewTargetView view = handlerRegistry.loadView(target, request.getReviewType());
        if (view == null) {
            /*
             * Handler 存在（否则提交阶段就不会放行），但按这个 revisionRef 拿不到内容。
             * 这属于 REVIEW_TARGET_REVISION_NOT_FOUND 而不是 REVIEW_TARGET_NOT_FOUND：
             * 语义差别是「目标对象还在，只是提交时那一版读不到了」，
             * 让 Reviewer 在看不到内容的情况下点通过，比返回错误危险得多。
             */
            throw new ReviewTargetRevisionNotFoundException();
        }
        return view;
    }

    // ---------- 映射 ----------

    private static ReviewListItemVO toListItem(ReviewRequestEntity entity) {
        return ReviewListItemVO.builder()
                .reviewId(entity.getId())
                .reviewType(entity.getReviewType())
                .targetModule(entity.getTargetModule())
                .targetType(entity.getTargetType())
                .targetId(entity.getTargetId())
                .targetDisplayName(entity.getTargetDisplayName())
                .applicantAccountId(entity.getApplicantAccountId())
                .applicantDisplayName(entity.getApplicantDisplayName())
                .status(entity.getStatus())
                .submittedAt(entity.getSubmittedAt())
                .build();
    }

    private static ReviewHistoryVO toHistory(ReviewRequestEntity entity) {
        return ReviewHistoryVO.builder()
                .reviewId(entity.getId())
                .reviewType(entity.getReviewType())
                .targetModule(entity.getTargetModule())
                .targetType(entity.getTargetType())
                .targetId(entity.getTargetId())
                .targetRevisionRef(entity.getTargetRevisionRef())
                .targetDisplayName(entity.getTargetDisplayName())
                .applicantAccountId(entity.getApplicantAccountId())
                .applicantDisplayName(entity.getApplicantDisplayName())
                .status(entity.getStatus())
                .reviewerAccountId(entity.getReviewerAccountId())
                .decisionReason(entity.getDecisionReason())
                .submittedAt(entity.getSubmittedAt())
                .decidedAt(entity.getDecidedAt())
                .canceledAt(entity.getCanceledAt())
                .build();
    }

    /*
     * 动作历史的展示名：只有「申请人自己做的动作」能填名字。
     * 决策类动作只有 reviewer_account_id，Account 模块没有暴露账户摘要 API，
     * 因此这里不编造名字，交给前端显示为「审核员 #<id>」。
     */
    private static ReviewActionVO toActionVO(ReviewActionEntity action, ReviewRequestEntity request) {
        boolean actorIsApplicant = action.getActorAccountId() != null
                && action.getActorAccountId().equals(request.getApplicantAccountId());
        boolean applicantAction = ReviewActionType.SUBMITTED_CODE.equals(action.getActionType())
                || ReviewActionType.CANCELED_CODE.equals(action.getActionType());
        String displayName = actorIsApplicant && applicantAction ? request.getApplicantDisplayName() : null;
        return ReviewActionVO.builder()
                .actionId(action.getId())
                .actionType(action.getActionType())
                .actorAccountId(action.getActorAccountId())
                .actorDisplayName(displayName)
                .actorType(action.getActorType() == null
                        ? ReviewActorType.ACCOUNT_CODE : action.getActorType())
                .note(action.getNote())
                .createdAt(action.getCreatedAt())
                .build();
    }

    // ---------- 参数校验 ----------

    private static int normalizePage(int page) {
        if (page < 1) {
            throw new ReviewQueryInvalidException("page 必须大于等于 1");
        }
        return page;
    }

    private static int normalizePageSize(int pageSize) {
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new ReviewQueryInvalidException("pageSize 必须在 1 到 " + MAX_PAGE_SIZE + " 之间");
        }
        return pageSize;
    }

    // 空白视为不筛选；非空但取值非法时明确报错
    private static String normalizeStatus(String status) {
        String trimmed = trimToNull(status);
        if (trimmed == null) {
            return null;
        }
        String normalized = trimmed.toUpperCase(Locale.ROOT);
        if (!ReviewStatus.isKnown(normalized)) {
            throw new ReviewQueryInvalidException("status 取值非法：" + status);
        }
        return normalized;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
