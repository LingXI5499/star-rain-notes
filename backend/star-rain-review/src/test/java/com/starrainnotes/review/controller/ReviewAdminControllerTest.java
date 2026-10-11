/*
 * ReviewAdminControllerTest：审核模块的测试文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：CurrentActorApi、ApiException、GlobalApiExceptionHandler、PageResult、ReviewWebConfig、ReviewViewer、ReviewStatus。
 * 阅读入口：queryService（查询参数业务服务处理入口）；decisionService（决定业务服务处理入口）；submissionService（submission业务服务处理入口）；currentActorApi（提供当前认证操作者身份的跨模块接口处理入口）。
 */

package com.starrainnotes.review.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.common.handler.GlobalApiExceptionHandler;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.review.config.ReviewWebConfig;
import com.starrainnotes.review.context.ReviewViewer;
import com.starrainnotes.review.enumeration.ReviewStatus;
import com.starrainnotes.review.interceptor.CurrentReviewerIdArgumentResolver;
import com.starrainnotes.review.service.ReviewDecisionService;
import com.starrainnotes.review.service.ReviewQueryService;
import com.starrainnotes.review.service.ReviewSubmissionService;
import com.starrainnotes.review.service.ReviewViewerProvider;
import com.starrainnotes.review.service.impl.ReviewViewerProviderImpl;
import com.starrainnotes.review.vo.ReviewDetailVO;
import com.starrainnotes.review.vo.ReviewListItemVO;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/*
 * 审核中心 HTTP 层的测试。
 *
 * 覆盖三件事：
 *   1. 路由与响应结构：/api/admin/reviews/** 的每个 REV 端点都能被正确访问与序列化；
 *   2. 方法级权限：没有对应 authority 的账户被拒（403），且不会进入 Service；
 *   3. 决策人来自认证上下文：浏览器无法通过请求体把自己伪造成另一个 Reviewer。
 *
 * 注意 401 不在这里断言：401 由 Account 的唯一安全过滤链（URL 层 authenticated）产生，
 * 本测试只装配 Web MVC 层，没有过滤器链。未认证时 CurrentActorApi 抛 UNAUTHORIZED，
 * 这一条由 ReviewAdminControllerTest 之外的 CurrentReviewerIdArgumentResolverTest 覆盖，
 * 生产语义则由验收报告里的 curl 实测证明。
 */
/**
 * 审核后台控制器测试，属于审核模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
@SpringJUnitWebConfig(ReviewAdminControllerTest.TestConfig.class)
class ReviewAdminControllerTest {

    /**
     * 测试配置，属于审核模块。
     * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
     */
    @Configuration
    @EnableWebMvc
    @EnableMethodSecurity
    static class TestConfig {

        /*
         * 这里刻意不用 @ComponentScan：它会把 ReviewPersistenceConfig 一起扫进来，
         * 从而在没有数据源的 Web 层测试里注册 Mapper 并因缺 SqlSessionFactory 而启动失败。
         * 需要的三个 Bean（Controller、参数解析器、注册解析器的 WebMvcConfigurer）显式声明，
         * 既覆盖了「ReviewWebConfig 确实把解析器挂上」这条约束，也不牵连持久层。
         */

        /**
         * 查询参数业务服务处理入口。
         * @return 类型为 <code>ReviewQueryService</code> 的处理结果
         */
        @Bean
        ReviewQueryService queryService() {
            return mock(ReviewQueryService.class);
        }

        /**
         * 决定业务服务处理入口。
         * @return 类型为 <code>ReviewDecisionService</code> 的处理结果
         */
        @Bean
        ReviewDecisionService decisionService() {
            return mock(ReviewDecisionService.class);
        }

        /**
         * submission业务服务处理入口。
         * @return 类型为 <code>ReviewSubmissionService</code> 的处理结果
         */
        @Bean
        ReviewSubmissionService submissionService() {
            return mock(ReviewSubmissionService.class);
        }

        /**
         * 提供当前认证操作者身份的跨模块接口处理入口。
         * @return 类型为 <code>CurrentActorApi</code> 的处理结果
         */
        @Bean
        CurrentActorApi currentActorApi() {
            return mock(CurrentActorApi.class);
        }

        /**
         * 审核Viewer提供方处理入口。
         *
         * @param currentActorApi 提供当前认证操作者身份的跨模块接口；类型为 <code>CurrentActorApi</code>
         * @return 类型为 <code>ReviewViewerProvider</code> 的处理结果
         */
        @Bean
        ReviewViewerProvider reviewViewerProvider(CurrentActorApi currentActorApi) {
            return new ReviewViewerProviderImpl(currentActorApi);
        }

        /*
         * 解析器在 interceptor 包，不在 TestConfig 声明的 config 包内，
         * 因此这里显式注册。生产里它由 boot 的主扫描（com.starrainnotes）收集为 @Component。
         */
        /**
         * 当前审核人标识ArgumentResolver处理入口。
         *
         * @param currentActorApi 提供当前认证操作者身份的跨模块接口；类型为 <code>CurrentActorApi</code>
         * @return 类型为 <code>CurrentReviewerIdArgumentResolver</code> 的处理结果
         */
        @Bean
        CurrentReviewerIdArgumentResolver currentReviewerIdArgumentResolver(CurrentActorApi currentActorApi) {
            return new CurrentReviewerIdArgumentResolver(currentActorApi);
        }

        /*
         * ReviewWebConfig 是 @CurrentReviewerId 解析器真正的注册点，
         * 这里显式建它（而不是 @ComponentScan 整个 config 包），
         * 让测试同时证明「解析器确实被挂到了 WebMvcConfigurer 上」。
         */
        /**
         * 审核Web配置处理入口。
         *
         * @param currentReviewerIdArgumentResolver 当前审核人标识ArgumentResolver；类型为 <code>CurrentReviewerIdArgumentResolver</code>
         * @return 类型为 <code>ReviewWebConfig</code> 的处理结果
         */
        @Bean
        ReviewWebConfig reviewWebConfig(CurrentReviewerIdArgumentResolver currentReviewerIdArgumentResolver) {
            return new ReviewWebConfig(currentReviewerIdArgumentResolver);
        }

        /**
         * 审核后台控制器处理入口。
         *
         * @param queryService 查询参数业务服务；类型为 <code>ReviewQueryService</code>
         * @param decisionService 决定业务服务；类型为 <code>ReviewDecisionService</code>
         * @param submissionService submission业务服务；类型为 <code>ReviewSubmissionService</code>
         * @param reviewViewerProvider 审核Viewer提供方；类型为 <code>ReviewViewerProvider</code>
         * @return 类型为 <code>ReviewAdminController</code> 的处理结果
         */
        @Bean
        ReviewAdminController reviewAdminController(ReviewQueryService queryService,
                                                   ReviewDecisionService decisionService,
                                                   ReviewSubmissionService submissionService,
                                                   ReviewViewerProvider reviewViewerProvider) {
            return new ReviewAdminController(queryService, decisionService, submissionService,
                    reviewViewerProvider);
        }

        // 复用 common 的全局异常处理器，保证「业务错误码 → HTTP 状态」与生产一致
        /**
         * global接口异常处理器处理入口。
         * @return 类型为 <code>GlobalApiExceptionHandler</code> 的处理结果
         */
        @Bean
        GlobalApiExceptionHandler globalApiExceptionHandler() {
            return new GlobalApiExceptionHandler();
        }
    }

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
     * 提供当前认证操作者身份的跨模块接口。
     * 协作类型：<code>CurrentActorApi</code>。
     */
    private final CurrentActorApi currentActorApi;
    /**
     * mockMvc。
     */
    private final MockMvc mockMvc;

    /**
     * 构造ReviewAdminControllerTest，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>MockMvcBuilders.webAppContextSetup</code>、<code>context.getBean</code>。
     *
     * @param context context；类型为 <code>WebApplicationContext</code>
     */
    ReviewAdminControllerTest(WebApplicationContext context) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        this.queryService = context.getBean(ReviewQueryService.class);
        this.decisionService = context.getBean(ReviewDecisionService.class);
        this.submissionService = context.getBean(ReviewSubmissionService.class);
        this.currentActorApi = context.getBean(CurrentActorApi.class);
    }

    /**
     * 重置Mocks。
     * 关键处理步骤调用 <code>org.mockito.Mockito.reset</code>。
     */
    @BeforeEach
    void resetMocks() {
        org.mockito.Mockito.reset(queryService, decisionService, submissionService,
                currentActorApi);
    }

    /**
     * 已认证As处理入口。
     * 主要协作调用：<code>currentActorApi.current</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>
     * @param permissions 权限集合；类型为 <code>String[]</code>
     */
    private void authenticatedAs(long accountId, String... permissions) {
        when(currentActorApi.current()).thenReturn(CurrentActorApi.CurrentActor.builder()
                .accountId(accountId)
                .roles(Set.of("SUPER_ADMIN"))
                .permissions(Set.of(permissions))
                .build());
    }

    /**
     * REV-002 \u5f85\u5ba1\u6838\u5217\u8868\uff1a\u6709 review:read \u65f6\u8fd4\u56de\u5206\u9875\u7ed3\u6784。
     * 主要协作调用：<code>queryService.pagePending</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test
    @WithMockUser(username = "admin", authorities = "review:read")
    @DisplayName("REV-002 待审核列表：有 review:read 时返回分页结构")
    void pendingAllowedWithReviewReadAuthority() throws Exception {
        when(queryService.pagePending(any())).thenReturn(new PageResult<>(
                List.of(ReviewListItemVO.builder().reviewId(500L)
                        .targetDisplayName("《Java 程序设计》")
                        .applicantDisplayName("账户 #9")
                        .status(ReviewStatus.PENDING_CODE).build()),
                1L, 1, 20));

        mockMvc.perform(get("/api/admin/reviews/pending").param("page", "1").param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].reviewId").value(500))
                .andExpect(jsonPath("$.data.items[0].targetDisplayName").value("《Java 程序设计》"));
    }

    /**
     * REV-002 \u7f3a\u5c11 review:read\uff1a403\uff0c\u4e14\u4e0d\u8fdb\u5165 Service。
     * 关键处理步骤调用 <code>get</code>、<code>status</code>、<code>jsonPath</code>、<code>never</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test
    @WithMockUser(username = "admin", authorities = "media:read")
    @DisplayName("REV-002 缺少 review:read：403，且不进入 Service")
    void pendingDeniedWithoutReviewReadAuthority() throws Exception {
        mockMvc.perform(get("/api/admin/reviews/pending"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        verify(queryService, never()).pagePending(any());
    }

    /**
     * REV-007 \u5ba1\u6838\u5386\u53f2\uff1a\u6709 review:history-read \u65f6\u8fd4\u56de\u5206\u9875\u7ed3\u6784。
     * 主要协作调用：<code>queryService.pageHistory</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test
    @WithMockUser(username = "admin", authorities = "review:history-read")
    @DisplayName("REV-007 审核历史：有 review:history-read 时返回分页结构")
    void historyAllowedWithHistoryReadAuthority() throws Exception {
        when(queryService.pageHistory(any())).thenReturn(new PageResult<>(List.of(), 0L, 1, 20));

        mockMvc.perform(get("/api/admin/reviews/history").param("targetId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    /**
     * REV-007 \u7f3a\u5c11 review:history-read\uff1a403。
     * 关键处理步骤调用 <code>get</code>、<code>status</code>、<code>never</code>、<code>any</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test
    @WithMockUser(username = "admin", authorities = "review:read")
    @DisplayName("REV-007 缺少 review:history-read：403")
    void historyDeniedWithoutHistoryRead() throws Exception {
        mockMvc.perform(get("/api/admin/reviews/history"))
                .andExpect(status().isForbidden());

        verify(queryService, never()).pageHistory(any());
    }

    /**
     * REV-004/005 \u51b3\u7b56\u7c7b\u7aef\u70b9\u5728\u6ca1\u6709\u53ef\u89e3\u6790\u4e3b\u4f53\u65f6\u4e0d\u4f1a\u6267\u884c Service\uff08\u771f\u6b63\u7684\u6743\u9650\u5224\u5b9a\u89c1 SecurityTest\uff09。
     * 关键处理步骤调用 <code>post</code>、<code>status</code>、<code>never</code>、<code>any</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test
    @WithMockUser(username = "owner", authorities = "review:read")
    @DisplayName("REV-004/005 决策类端点在没有可解析主体时不会执行 Service（真正的权限判定见 SecurityTest）")
    void decisionEndpointsNeverReachServiceWithoutActor() throws Exception {
        mockMvc.perform(post("/api/admin/reviews/500/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"通过\"}"))
                .andExpect(status().isInternalServerError());
        mockMvc.perform(post("/api/admin/reviews/500/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"不合格\"}"))
                .andExpect(status().isInternalServerError());

        verify(decisionService, never()).approve(any(), any(), any());
        verify(decisionService, never()).reject(any(), any(), any());
    }

    /**
     * REV-004 SUPER_ADMIN \u901a\u8fc7\uff1a\u8bf7\u6c42\u6210\u529f\uff0c\u51b3\u7b56\u4eba\u6765\u81ea\u8ba4\u8bc1\u4e0a\u4e0b\u6587\u800c\u4e0d\u662f\u8bf7\u6c42\u4f53。
     * 关键处理步骤调用 <code>authenticatedAs</code>、<code>post</code>、<code>status</code>、<code>jsonPath</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test
    @WithMockUser(username = "super", authorities = {"review:read", "review:approve"})
    @DisplayName("REV-004 SUPER_ADMIN 通过：请求成功，决策人来自认证上下文而不是请求体")
    void approveSucceedsForSuperAdmin() throws Exception {
        authenticatedAs(1L, "review:read", "review:approve");

        mockMvc.perform(post("/api/admin/reviews/500/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"审核通过\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        // 决策人恒等于认证上下文里的账户，签名里根本没有 reviewerAccountId 这个入参
        verify(decisionService).approve(eq(500L), any(), eq(1L));
    }

    /**
     * REV-005 \u62d2\u7edd\u7f3a\u5c11 reason\uff1a\u88ab\u6821\u9a8c\u62e6\u6210 400\uff0c\u4e0d\u8fdb\u5165 Service。
     * 关键处理步骤调用 <code>post</code>、<code>status</code>、<code>never</code>、<code>any</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test
    @WithMockUser(username = "super", authorities = {"review:read", "review:reject"})
    @DisplayName("REV-005 拒绝缺少 reason：被校验拦成 400，不进入 Service")
    void rejectRequiresReasonBody() throws Exception {
        mockMvc.perform(post("/api/admin/reviews/500/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(decisionService, never()).reject(any(), any(), any());
    }

    /**
     * REV-005 SUPER_ADMIN \u62d2\u7edd\u6210\u529f\uff1areason \u539f\u6837\u4f20\u7ed9 Service。
     * 关键处理步骤调用 <code>authenticatedAs</code>、<code>post</code>、<code>status</code>、<code>eq</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test
    @WithMockUser(username = "super", authorities = {"review:read", "review:reject"})
    @DisplayName("REV-005 SUPER_ADMIN 拒绝成功：reason 原样传给 Service")
    void rejectSucceedsForSuperAdmin() throws Exception {
        authenticatedAs(1L, "review:read", "review:reject");

        mockMvc.perform(post("/api/admin/reviews/500/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"章节3示例代码无法运行\"}"))
                .andExpect(status().isOk());

        verify(decisionService).reject(eq(500L), any(), eq(1L));
    }

    /**
     * REV-003 \u8be6\u60c5\uff1a\u52a8\u4f5c\u5f00\u5173\u7531\u670d\u52a1\u7aef\u8ba1\u7b97\uff0c\u524d\u7aef\u4e0d\u91cd\u7b97\u6388\u6743\u89c4\u5219。
     * 主要协作调用：<code>queryService.getDetail</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test
    @WithMockUser(username = "owner", authorities = "review:read")
    @DisplayName("REV-003 详情：动作开关由服务端计算，前端不重算授权规则")
    void detailReturnsServerSideActionFlags() throws Exception {
        authenticatedAs(9L, "review:read");
        when(queryService.getDetail(eq(500L), any(ReviewViewer.class)))
                .thenReturn(ReviewDetailVO.builder()
                        .reviewId(500L)
                        .status(ReviewStatus.PENDING_CODE)
                        .canApprove(false)
                        .canReject(false)
                        .canCancel(true)
                        .history(List.of())
                        .build());

        mockMvc.perform(get("/api/admin/reviews/500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewId").value(500))
                .andExpect(jsonPath("$.data.canApprove").value(false))
                .andExpect(jsonPath("$.data.canCancel").value(true));
    }

    /**
     * REV-003 \u8be6\u60c5\uff1aService \u629b REVIEW_ACCESS_DENIED \u65f6\u6620\u5c04\u4e3a 403 \u4e0e\u4e1a\u52a1\u9519\u8bef\u7801。
     * 主要协作调用：<code>queryService.getDetail</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test
    @WithMockUser(username = "owner", authorities = "review:read")
    @DisplayName("REV-003 详情：Service 抛 REVIEW_ACCESS_DENIED 时映射为 403 与业务错误码")
    void detailMapsAccessDeniedBusinessCode() throws Exception {
        authenticatedAs(77L, "review:read");
        when(queryService.getDetail(eq(500L), any(ReviewViewer.class)))
                .thenThrow(new ApiException("REVIEW_ACCESS_DENIED", "没有查看该审核请求的权限", 403));

        mockMvc.perform(get("/api/admin/reviews/500"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("REVIEW_ACCESS_DENIED"));
    }

    /**
     * REV-006 \u53d6\u6d88\uff1a\u7533\u8bf7\u4eba\u672c\u4eba\u8c03\u7528\u53d6\u6d88\uff0cactor \u53d6\u81ea\u8ba4\u8bc1\u4e0a\u4e0b\u6587。
     * 关键处理步骤调用 <code>authenticatedAs</code>、<code>post</code>、<code>status</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test
    @WithMockUser(username = "owner", authorities = "review:read")
    @DisplayName("REV-006 取消：申请人本人调用取消，actor 取自认证上下文")
    void cancelUsesAuthenticatedActor() throws Exception {
        authenticatedAs(9L, "review:read");

        mockMvc.perform(post("/api/admin/reviews/500/cancel"))
                .andExpect(status().isOk());

        verify(submissionService).cancelByApplicant(500L, 9L);
    }

}
