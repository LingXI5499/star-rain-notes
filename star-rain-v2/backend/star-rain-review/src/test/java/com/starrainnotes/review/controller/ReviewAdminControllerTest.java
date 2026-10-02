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
import com.starrainnotes.review.api.dto.ReviewSubmissionResult;
import com.starrainnotes.review.config.ReviewWebConfig;
import com.starrainnotes.review.context.ReviewViewer;
import com.starrainnotes.review.enumeration.ReviewStatus;
import com.starrainnotes.review.handler.impl.ReviewDemoTargetHandler;
import com.starrainnotes.review.interceptor.CurrentReviewerIdArgumentResolver;
import com.starrainnotes.review.service.ReviewDecisionService;
import com.starrainnotes.review.service.ReviewDemoSubmissionService;
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
@SpringJUnitWebConfig(ReviewAdminControllerTest.TestConfig.class)
class ReviewAdminControllerTest {

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

        @Bean
        ReviewQueryService queryService() {
            return mock(ReviewQueryService.class);
        }

        @Bean
        ReviewDecisionService decisionService() {
            return mock(ReviewDecisionService.class);
        }

        @Bean
        ReviewSubmissionService submissionService() {
            return mock(ReviewSubmissionService.class);
        }

        @Bean
        ReviewDemoSubmissionService demoSubmissionService() {
            return mock(ReviewDemoSubmissionService.class);
        }

        @Bean
        CurrentActorApi currentActorApi() {
            return mock(CurrentActorApi.class);
        }

        @Bean
        ReviewViewerProvider reviewViewerProvider(CurrentActorApi currentActorApi) {
            return new ReviewViewerProviderImpl(currentActorApi);
        }

        /*
         * 解析器在 interceptor 包，不在 TestConfig 声明的 config 包内，
         * 因此这里显式注册。生产里它由 boot 的主扫描（com.starrainnotes）收集为 @Component。
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
        @Bean
        ReviewWebConfig reviewWebConfig(CurrentReviewerIdArgumentResolver currentReviewerIdArgumentResolver) {
            return new ReviewWebConfig(currentReviewerIdArgumentResolver);
        }

        @Bean
        ReviewAdminController reviewAdminController(ReviewQueryService queryService,
                                                   ReviewDecisionService decisionService,
                                                   ReviewSubmissionService submissionService,
                                                   ReviewDemoSubmissionService demoSubmissionService,
                                                   ReviewViewerProvider reviewViewerProvider) {
            return new ReviewAdminController(queryService, decisionService, submissionService,
                    demoSubmissionService, reviewViewerProvider);
        }

        // 复用 common 的全局异常处理器，保证「业务错误码 → HTTP 状态」与生产一致
        @Bean
        GlobalApiExceptionHandler globalApiExceptionHandler() {
            return new GlobalApiExceptionHandler();
        }
    }

    private final ReviewQueryService queryService;
    private final ReviewDecisionService decisionService;
    private final ReviewSubmissionService submissionService;
    private final ReviewDemoSubmissionService demoSubmissionService;
    private final CurrentActorApi currentActorApi;
    private final MockMvc mockMvc;

    ReviewAdminControllerTest(WebApplicationContext context) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        this.queryService = context.getBean(ReviewQueryService.class);
        this.decisionService = context.getBean(ReviewDecisionService.class);
        this.submissionService = context.getBean(ReviewSubmissionService.class);
        this.demoSubmissionService = context.getBean(ReviewDemoSubmissionService.class);
        this.currentActorApi = context.getBean(CurrentActorApi.class);
    }

    @BeforeEach
    void resetMocks() {
        org.mockito.Mockito.reset(queryService, decisionService, submissionService,
                demoSubmissionService, currentActorApi);
    }

    private void authenticatedAs(long accountId, String... permissions) {
        when(currentActorApi.current()).thenReturn(CurrentActorApi.CurrentActor.builder()
                .accountId(accountId)
                .roles(Set.of("SUPER_ADMIN"))
                .permissions(Set.of(permissions))
                .build());
    }

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

    @Test
    @WithMockUser(username = "admin", authorities = "media:read")
    @DisplayName("REV-002 缺少 review:read：403，且不进入 Service")
    void pendingDeniedWithoutReviewReadAuthority() throws Exception {
        mockMvc.perform(get("/api/admin/reviews/pending"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        verify(queryService, never()).pagePending(any());
    }

    @Test
    @WithMockUser(username = "admin", authorities = "review:history-read")
    @DisplayName("REV-007 审核历史：有 review:history-read 时返回分页结构")
    void historyAllowedWithHistoryReadAuthority() throws Exception {
        when(queryService.pageHistory(any())).thenReturn(new PageResult<>(List.of(), 0L, 1, 20));

        mockMvc.perform(get("/api/admin/reviews/history").param("targetId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    @WithMockUser(username = "admin", authorities = "review:read")
    @DisplayName("REV-007 缺少 review:history-read：403")
    void historyDeniedWithoutHistoryRead() throws Exception {
        mockMvc.perform(get("/api/admin/reviews/history"))
                .andExpect(status().isForbidden());

        verify(queryService, never()).pageHistory(any());
    }

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

    @Test
    @WithMockUser(username = "owner", authorities = "review:read")
    @DisplayName("REV-006 取消：申请人本人调用取消，actor 取自认证上下文")
    void cancelUsesAuthenticatedActor() throws Exception {
        authenticatedAs(9L, "review:read");

        mockMvc.perform(post("/api/admin/reviews/500/cancel"))
                .andExpect(status().isOk());

        verify(submissionService).cancelByApplicant(500L, 9L);
    }

    @Test
    @WithMockUser(username = "super", authorities = "review:read")
    @DisplayName("REV-001 演示提交入口：申请人取自认证上下文，返回 201")
    void demoSubmissionUsesAuthenticatedApplicant() throws Exception {
        authenticatedAs(9L, "review:read");
        when(demoSubmissionService.submit(any(), eq(9L))).thenReturn(ReviewSubmissionResult.builder()
                .reviewRequestId(500L)
                .status(ReviewStatus.PENDING_CODE)
                .reviewType(ReviewDemoTargetHandler.REVIEW_TYPE)
                .build());

        mockMvc.perform(post("/api/admin/reviews/demo-submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reviewType\":\"demo.publish\",\"targetModule\":\"DEMO\","
                                + "\"targetType\":\"DEMO_TARGET\",\"targetId\":100,"
                                + "\"targetRevisionRef\":\"revision:7\","
                                + "\"targetDisplayName\":\"演示目标\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.reviewRequestId").value(500))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        verify(demoSubmissionService).submit(any(), eq(9L));
    }

    @Test
    @WithMockUser(username = "nobody", authorities = "media:read")
    @DisplayName("REV-001 演示提交入口要求 review:read；无权时 Service 不会被调用")
    void demoSubmissionDeniedWithoutReviewRead() throws Exception {
        /*
         * 本上下文只装配 Web MVC 层，没有 Spring Security 的方法级拦截器，
         * 因此这里断言的是「请求没有抵达演示提交服务」；
         * 「缺少 review:read 会被 403」由 ReviewAdminControllerSecurityTest 对
         * @PreAuthorize 声明求值来精确证明。
         */
        mockMvc.perform(post("/api/admin/reviews/demo-submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reviewType\":\"demo.publish\",\"targetModule\":\"DEMO\","
                                + "\"targetType\":\"DEMO_TARGET\",\"targetId\":100,"
                                + "\"targetRevisionRef\":\"revision:7\","
                                + "\"targetDisplayName\":\"演示目标\"}"))
                .andExpect(status().isInternalServerError());

        verify(demoSubmissionService, never()).submit(any(), any());
    }
}
