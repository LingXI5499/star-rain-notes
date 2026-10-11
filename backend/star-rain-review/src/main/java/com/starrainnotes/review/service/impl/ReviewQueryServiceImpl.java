/*
 * ReviewQueryServiceImpl：审核模块的业务实现文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：PageResult、ReviewTargetRef、ReviewTargetView、ReviewPermissions、ReviewViewer、ReviewHistoryQueryDTO、ReviewQueryDTO。
 * 阅读入口：pagePending（分页读取待处理）；getDetail（读取详情）；pageHistory（分页读取历史）。
 */

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
/**
 * 审核查询参数业务服务实现，属于审核模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
public class ReviewQueryServiceImpl implements ReviewQueryService {

    /**
     * 大小。
     * 初始值为 <code>100</code>。
     */
    private static final int MAX_PAGE_SIZE = 100;

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
     * 构造ReviewQueryServiceImpl，接收本类使用的依赖或初始化参数。
     *
     * @param requestMapper 请求数据映射；类型为 <code>ReviewRequestMapper</code>
     * @param actionMapper 动作数据映射；类型为 <code>ReviewActionMapper</code>
     * @param handlerRegistry 处理器Registry；类型为 <code>ReviewTargetHandlerRegistry</code>
     */
    public ReviewQueryServiceImpl(ReviewRequestMapper requestMapper,
                                  ReviewActionMapper actionMapper,
                                  ReviewTargetHandlerRegistry handlerRegistry) {
        this.requestMapper = requestMapper;
        this.actionMapper = actionMapper;
        this.handlerRegistry = handlerRegistry;
    }

    /**
     * 分页读取待处理。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>requestMapper.pendingCount</code>、<code>requestMapper.pendingPage</code>。
     *
     * @param query 查询参数；类型为 <code>ReviewQueryDTO</code>
     * @return 类型为 <code>PageResult&lt;ReviewListItemVO&gt;</code> 的处理结果
     */
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

    /**
     * 读取详情。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>requestMapper.requestById</code>、<code>actionMapper.selectByReviewRequestId</code>。
     * 实现中的显式失败类型包括 <code>ReviewNotFoundException</code>、<code>ReviewAccessDeniedException</code>；具体触发条件见对应分支。
     *
     * @param reviewRequestId 审核请求标识；类型为 <code>Long</code>
     * @param viewer viewer；类型为 <code>ReviewViewer</code>
     * @return 类型为 <code>ReviewDetailVO</code> 的处理结果
     */
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

    /**
     * 分页读取历史。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>requestMapper.historyCount</code>、<code>requestMapper.historyPage</code>。
     * 实现中的显式失败类型包括 <code>ReviewQueryInvalidException</code>；具体触发条件见对应分支。
     *
     * @param query 查询参数；类型为 <code>ReviewHistoryQueryDTO</code>
     * @return 类型为 <code>PageResult&lt;ReviewHistoryVO&gt;</code> 的处理结果
     */
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
    /**
     * 判断能否View。
     * 关键处理步骤调用 <code>viewer.isApplicant</code>、<code>request.getApplicantAccountId</code>、<code>viewer.reviewer</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>ReviewRequestEntity</code>
     * @param viewer viewer；类型为 <code>ReviewViewer</code>
     * @return 本方法条件是否成立的判断结果
     */
    private static boolean canView(ReviewRequestEntity request, ReviewViewer viewer) {
        if (viewer == null) {
            return false;
        }
        return viewer.isApplicant(request.getApplicantAccountId()) || viewer.reviewer();
    }

    // 只有 PENDING 才谈得上「可以决定」，终态一律不给按钮
    /**
     * 判断能否Decide。
     * 关键处理步骤调用 <code>ReviewStatus.PENDING_CODE.equals</code>、<code>request.getStatus</code>、<code>viewer.hasPermission</code>。
     *
     * @param viewer viewer；类型为 <code>ReviewViewer</code>
     * @param request 本次操作的请求参数对象；类型为 <code>ReviewRequestEntity</code>
     * @param permission 权限；类型为 <code>String</code>
     * @return 本方法条件是否成立的判断结果
     */
    private static boolean canDecide(ReviewViewer viewer, ReviewRequestEntity request, String permission) {
        return viewer != null
                && ReviewStatus.PENDING_CODE.equals(request.getStatus())
                && viewer.hasPermission(permission);
    }

    // 取消只属于申请人本人：Reviewer 不应该替申请人撤回请求
    /**
     * 判断能否Cancel。
     * 关键处理步骤调用 <code>ReviewStatus.PENDING_CODE.equals</code>、<code>request.getStatus</code>、<code>viewer.isApplicant</code>、<code>request.getApplicantAccountId</code>。
     *
     * @param viewer viewer；类型为 <code>ReviewViewer</code>
     * @param request 本次操作的请求参数对象；类型为 <code>ReviewRequestEntity</code>
     * @return 本方法条件是否成立的判断结果
     */
    private static boolean canCancel(ReviewViewer viewer, ReviewRequestEntity request) {
        return viewer != null
                && ReviewStatus.PENDING_CODE.equals(request.getStatus())
                && viewer.isApplicant(request.getApplicantAccountId());
    }

    // ---------- 目标视图 ----------

    /**
     * 加载目标View。
     * 主要协作调用：<code>handlerRegistry.loadView</code>。
     * 实现中的显式失败类型包括 <code>ReviewTargetRevisionNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>ReviewRequestEntity</code>
     * @return 类型为 <code>ReviewTargetView</code> 的处理结果
     */
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

    /**
     * 转换为ListItem。
     * 关键处理步骤调用 <code>ReviewListItemVO.builder</code>、<code>entity.getId</code>、<code>entity.getReviewType</code>、<code>entity.getTargetModule</code>。
     *
     * @param entity 数据库映射实体；类型为 <code>ReviewRequestEntity</code>
     * @return 类型为 <code>ReviewListItemVO</code> 的处理结果
     */
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

    /**
     * 转换为历史。
     * 关键处理步骤调用 <code>ReviewHistoryVO.builder</code>、<code>entity.getId</code>、<code>entity.getReviewType</code>、<code>entity.getTargetModule</code>。
     *
     * @param entity 数据库映射实体；类型为 <code>ReviewRequestEntity</code>
     * @return 类型为 <code>ReviewHistoryVO</code> 的处理结果
     */
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
    /**
     * 转换为动作响应对象。
     * 关键处理步骤调用 <code>action.getActorAccountId</code>、<code>request.getApplicantAccountId</code>、<code>ReviewActionType.SUBMITTED_CODE.equals</code>、<code>action.getActionType</code>。
     *
     * @param action 动作；类型为 <code>ReviewActionEntity</code>
     * @param request 本次操作的请求参数对象；类型为 <code>ReviewRequestEntity</code>
     * @return 类型为 <code>ReviewActionVO</code> 的处理结果
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

    /**
     * 规范化分页。
     * 实现中的显式失败类型包括 <code>ReviewQueryInvalidException</code>；具体触发条件见对应分支。
     *
     * @param page 分页页码；类型为 <code>int</code>
     * @return 类型为 <code>int</code> 的处理结果
     */
    private static int normalizePage(int page) {
        if (page < 1) {
            throw new ReviewQueryInvalidException("page 必须大于等于 1");
        }
        return page;
    }

    /**
     * 规范化分页大小。
     * 实现中的显式失败类型包括 <code>ReviewQueryInvalidException</code>；具体触发条件见对应分支。
     *
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @return 类型为 <code>int</code> 的处理结果
     */
    private static int normalizePageSize(int pageSize) {
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new ReviewQueryInvalidException("pageSize 必须在 1 到 " + MAX_PAGE_SIZE + " 之间");
        }
        return pageSize;
    }

    // 空白视为不筛选；非空但取值非法时明确报错
    /**
     * 规范化状态。
     * 关键处理步骤调用 <code>trimToNull</code>、<code>trimmed.toUpperCase</code>、<code>ReviewStatus.isKnown</code>。
     * 实现中的显式失败类型包括 <code>ReviewQueryInvalidException</code>；具体触发条件见对应分支。
     *
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
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

    /**
     * 去除首尾空白目标空值处理入口。
     * 关键处理步骤调用 <code>value.trim</code>、<code>trimmed.isEmpty</code>。
     *
     * @param value 值；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
