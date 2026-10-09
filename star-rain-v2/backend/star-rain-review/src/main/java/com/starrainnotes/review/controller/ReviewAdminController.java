package com.starrainnotes.review.controller;

import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.review.dto.ReviewApproveCommand;
import com.starrainnotes.review.dto.ReviewHistoryQueryDTO;
import com.starrainnotes.review.dto.ReviewQueryDTO;
import com.starrainnotes.review.dto.ReviewRejectCommand;
import com.starrainnotes.review.interceptor.CurrentReviewerId;
import com.starrainnotes.review.service.ReviewDecisionService;
import com.starrainnotes.review.service.ReviewQueryService;
import com.starrainnotes.review.service.ReviewSubmissionService;
import com.starrainnotes.review.service.ReviewViewerProvider;
import com.starrainnotes.review.vo.ReviewDetailVO;
import com.starrainnotes.review.vo.ReviewHistoryVO;
import com.starrainnotes.review.vo.ReviewListItemVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * 审核中心后台接口：REV-001 ~ REV-007。
 *
 * URL 层只要求「已认证」（见 ReviewSecurityContributor），
 * 具体权限由这里的 @PreAuthorize 逐个端点声明，因此普通 ADMIN 能看待审列表，
 * 但 review:approve / review:reject 当前只授予 SUPER_ADMIN。
 *
 * Controller 只做三件事：接参、转调 Service、包 ApiResponse。
 * 它不判断目标版本、不直接改 status、更不碰任何业务模块的 Mapper。
 *
 * 决策人从 @CurrentReviewerId 注入，来自认证上下文而不是请求体，
 * 浏览器无法把自己伪造成另一个 Reviewer。
 */
@RestController
@RequestMapping("/api/admin/reviews")
public class ReviewAdminController {

    private final ReviewQueryService queryService;
    private final ReviewDecisionService decisionService;
    private final ReviewSubmissionService submissionService;
    private final ReviewViewerProvider viewerProvider;

    public ReviewAdminController(ReviewQueryService queryService,
                                 ReviewDecisionService decisionService,
                                 ReviewSubmissionService submissionService,
                                 ReviewViewerProvider viewerProvider) {
        this.queryService = queryService;
        this.decisionService = decisionService;
        this.submissionService = submissionService;
        this.viewerProvider = viewerProvider;
    }

    // REV-002 查看待审核列表
    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('review:read')")
    public ApiResponse<PageResult<ReviewListItemVO>> pending(ReviewQueryDTO query) {
        return ApiResponse.ok(queryService.pagePending(query));
    }

    /*
     * REV-007 查询审核历史。
     *
     * 用独立路径 /history 而不是把两种查询塞进同一个端点：
     * 历史的筛选维度与待办完全不同，混在一起会出现「传了 status=PENDING
     * 却走了待办索引」这类难以排查的歧义。
     */
    @GetMapping("/history")
    @PreAuthorize("hasAuthority('review:history-read')")
    public ApiResponse<PageResult<ReviewHistoryVO>> history(ReviewHistoryQueryDTO query) {
        return ApiResponse.ok(queryService.pageHistory(query));
    }

    // REV-003 查看审核详情（申请人本人也可访问，对象级授权在 Service 内判定）
    @GetMapping("/{reviewId}")
    @PreAuthorize("hasAuthority('review:read')")
    public ApiResponse<ReviewDetailVO> detail(@PathVariable Long reviewId) {
        return ApiResponse.ok(queryService.getDetail(reviewId, viewerProvider.current()));
    }

    // REV-004 审核通过
    @PostMapping("/{reviewId}/approve")
    @PreAuthorize("hasAuthority('review:approve')")
    public ApiResponse<Void> approve(@PathVariable Long reviewId,
                                     @Valid @RequestBody(required = false) ReviewApproveCommand command,
                                     @CurrentReviewerId Long reviewerAccountId) {
        decisionService.approve(reviewId, command == null ? new ReviewApproveCommand() : command,
                reviewerAccountId);
        return ApiResponse.ok(null);
    }

    // REV-005 审核拒绝：reason 必填，缺失时由 @Valid 拦成 INVALID_REQUEST，Service 再兜一层
    @PostMapping("/{reviewId}/reject")
    @PreAuthorize("hasAuthority('review:reject')")
    public ApiResponse<Void> reject(@PathVariable Long reviewId,
                                    @Valid @RequestBody ReviewRejectCommand command,
                                    @CurrentReviewerId Long reviewerAccountId) {
        decisionService.reject(reviewId, command, reviewerAccountId);
        return ApiResponse.ok(null);
    }

    // REV-006 取消待审请求：只有申请人本人能取消自己提交的请求
    @PostMapping("/{reviewId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> cancel(@PathVariable Long reviewId,
                                    @CurrentReviewerId Long actorAccountId) {
        submissionService.cancelByApplicant(reviewId, actorAccountId);
        return ApiResponse.ok(null);
    }

}
