/*
 * ReviewAdminController：审核模块的HTTP 接入文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：ApiResponse、PageResult、ReviewApproveCommand、ReviewHistoryQueryDTO、ReviewQueryDTO、ReviewRejectCommand、CurrentReviewerId。
 * 阅读入口：pending（待处理处理入口）；history（历史处理入口）；detail（读取详情审核后台）；approve（批准审核后台）。
 */

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
/**
 * 审核后台控制器，属于审核模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequestMapping("/api/admin/reviews")
public class ReviewAdminController {

    /**
     * 查询参数业务服务。
     * 协作类型：<code>ReviewQueryService</code>。
     */
    private final ReviewQueryService queryService;
    /**
     * 决定业务服务。
     * 协作类型：<code>ReviewDecisionService</code>。
     */
    private final ReviewDecisionService decisionService;
    /**
     * submission业务服务。
     * 协作类型：<code>ReviewSubmissionService</code>。
     */
    private final ReviewSubmissionService submissionService;
    /**
     * viewer提供方。
     */
    private final ReviewViewerProvider viewerProvider;

    /**
     * 构造ReviewAdminController，接收本类使用的依赖或初始化参数。
     *
     * @param queryService 查询参数业务服务；类型为 <code>ReviewQueryService</code>
     * @param decisionService 决定业务服务；类型为 <code>ReviewDecisionService</code>
     * @param submissionService submission业务服务；类型为 <code>ReviewSubmissionService</code>
     * @param viewerProvider viewer提供方；类型为 <code>ReviewViewerProvider</code>
     */
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
    /**
     * 待处理处理入口。
     * HTTP 映射：<code>@GetMapping("/pending")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'review:read\')")</code>。
     * 主要协作调用：<code>queryService.pagePending</code>。
     *
     * @param query 查询参数；类型为 <code>ReviewQueryDTO</code>
     * @return 类型为 <code>ApiResponse&lt;PageResult&lt;ReviewListItemVO&gt;&gt;</code> 的处理结果
     */
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
    /**
     * 历史处理入口。
     * HTTP 映射：<code>@GetMapping("/history")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'review:history-read\')")</code>。
     * 主要协作调用：<code>queryService.pageHistory</code>。
     *
     * @param query 查询参数；类型为 <code>ReviewHistoryQueryDTO</code>
     * @return 类型为 <code>ApiResponse&lt;PageResult&lt;ReviewHistoryVO&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/history")
    @PreAuthorize("hasAuthority('review:history-read')")
    public ApiResponse<PageResult<ReviewHistoryVO>> history(ReviewHistoryQueryDTO query) {
        return ApiResponse.ok(queryService.pageHistory(query));
    }

    // REV-003 查看审核详情（申请人本人也可访问，对象级授权在 Service 内判定）
    /**
     * 读取详情审核后台。
     * HTTP 映射：<code>@GetMapping("/{reviewId}")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'review:read\')")</code>。
     * 主要协作调用：<code>queryService.getDetail</code>、<code>viewerProvider.current</code>。
     *
     * @param reviewId 审核标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;ReviewDetailVO&gt;</code> 的处理结果
     */
    @GetMapping("/{reviewId}")
    @PreAuthorize("hasAuthority('review:read')")
    public ApiResponse<ReviewDetailVO> detail(@PathVariable Long reviewId) {
        return ApiResponse.ok(queryService.getDetail(reviewId, viewerProvider.current()));
    }

    // REV-004 审核通过
    /**
     * 批准审核后台。
     * HTTP 映射：<code>@PostMapping("/{reviewId}/approve")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'review:approve\')")</code>。
     * 主要协作调用：<code>decisionService.approve</code>。
     *
     * @param reviewId 审核标识；类型为 <code>Long</code>
     * @param command command；类型为 <code>ReviewApproveCommand</code>；嵌套字段继续执行校验
     * @param reviewerAccountId 审核人账号标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;Void&gt;</code> 的处理结果
     */
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
    /**
     * recover处理入口。
     * HTTP 映射：<code>@PostMapping("/{reviewId}/recover")</code>。
     * 调用约束：<code>@PreAuthorize("hasRole(\'SUPER_ADMIN\') and hasAuthority(\'review:approve\')")</code>。
     * 主要协作调用：<code>decisionService.recoverApproved</code>。
     *
     * @param reviewId 审核标识；类型为 <code>Long</code>
     * @param reviewerAccountId 审核人账号标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;Void&gt;</code> 的处理结果
     */
    @PostMapping("/{reviewId}/recover")
    @PreAuthorize("hasRole('SUPER_ADMIN') and hasAuthority('review:approve')")
    public ApiResponse<Void> recover(@PathVariable Long reviewId, @CurrentReviewerId Long reviewerAccountId) {
        decisionService.recoverApproved(reviewId, reviewerAccountId);
        return ApiResponse.ok(null);
    }

    /**
     * 拒绝审核后台。
     * HTTP 映射：<code>@PostMapping("/{reviewId}/reject")</code>。
     * 调用约束：<code>@PreAuthorize("hasAuthority(\'review:reject\')")</code>。
     * 主要协作调用：<code>decisionService.reject</code>。
     *
     * @param reviewId 审核标识；类型为 <code>Long</code>
     * @param command command；类型为 <code>ReviewRejectCommand</code>；嵌套字段继续执行校验
     * @param reviewerAccountId 审核人账号标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;Void&gt;</code> 的处理结果
     */
    @PostMapping("/{reviewId}/reject")
    @PreAuthorize("hasAuthority('review:reject')")
    public ApiResponse<Void> reject(@PathVariable Long reviewId,
                                    @Valid @RequestBody ReviewRejectCommand command,
                                    @CurrentReviewerId Long reviewerAccountId) {
        decisionService.reject(reviewId, command, reviewerAccountId);
        return ApiResponse.ok(null);
    }

    // REV-006 取消待审请求：只有申请人本人能取消自己提交的请求
    /**
     * 取消审核后台。
     * HTTP 映射：<code>@PostMapping("/{reviewId}/cancel")</code>。
     * 调用约束：<code>@PreAuthorize("isAuthenticated()")</code>。
     * 主要协作调用：<code>submissionService.cancelByApplicant</code>。
     *
     * @param reviewId 审核标识；类型为 <code>Long</code>
     * @param actorAccountId 操作者账号标识；类型为 <code>Long</code>
     * @return 类型为 <code>ApiResponse&lt;Void&gt;</code> 的处理结果
     */
    @PostMapping("/{reviewId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> cancel(@PathVariable Long reviewId,
                                    @CurrentReviewerId Long actorAccountId) {
        submissionService.cancelByApplicant(reviewId, actorAccountId);
        return ApiResponse.ok(null);
    }

}
