package com.starrainnotes.review.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

/*
 * 审核中心的权限声明测试（越权拒绝的机械证明）。
 *
 * 为什么要单独测：
 *   方法级安全（@EnableMethodSecurity）只在真实安全链 + 真实 AOP 代理下生效，
 *   轻量的 Web 层测试上下文里 @PreAuthorize 不会被拦截。
 *   因此这里不搭 Spring 上下文，而是把 Controller 上真实声明的 SpEL 取出来，
 *   用 Spring Security 自己的 SecurityExpressionRoot 求值，
 *   从而机械地证明「普通 ADMIN 会被 hasAuthority 拒绝、SUPER_ADMIN 才会通过」。
 *
 * 覆盖口径与设计规范一致：
 *   /pending 需要 review:read          → ADMIN + SUPER_ADMIN
 *   /history 需要 review:history-read  → ADMIN + SUPER_ADMIN
 *   /{id}    需要 review:read          → ADMIN + SUPER_ADMIN（对象级授权在 Service 内）
 *   /approve 需要 review:approve       → 仅 SUPER_ADMIN
 *   /reject  需要 review:reject        → 仅 SUPER_ADMIN
 *   /cancel  需要 review:read + 申请人本人（身份判定在 Service 内）
 *   demo-submissions 需要 review:read
 */
class ReviewAdminControllerSecurityTest {

    private static final String ADMIN = "ROLE_ADMIN";
    private static final String SUPER_ADMIN = "ROLE_SUPER_ADMIN";

    private final ExpressionParser parser = new SpelExpressionParser();

    // 声明真实的 RoleHierarchy：SUPER_ADMIN 继承 ADMIN，与数据库里的角色设计一致
    private final RoleHierarchy roleHierarchy = RoleHierarchyImpl.fromHierarchy("ROLE_SUPER_ADMIN > ROLE_ADMIN");

    // 只有受保护的方法才需要 @PreAuthorize；这里挑出所有声明了它的端点
    private static Method method(String name, Class<?>... parameterTypes) {
        try {
            return ReviewAdminController.class.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException exception) {
            throw new AssertionError("Controller 方法签名发生变化：" + name, exception);
        }
    }

    private static String preAuthorize(Method method) {
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertThat(annotation).as("%s 必须声明 @PreAuthorize，否则 URL 层 authenticated 会被误当成授权",
                method.getName()).isNotNull();
        return annotation.value();
    }

    // 用 Spring Security 自己的表达式根求值，不自己实现 hasAuthority 语义
    private boolean allowed(Method method, String... authorities) {
        List<GrantedAuthority> granted = AuthorityUtils.createAuthorityList(authorities);
        Authentication authentication = new TestingAuthenticationToken("tester", "n/a", granted);
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            StandardEvaluationContext context = new StandardEvaluationContext();
            context.setRootObject(new TestRoot(authentication));
            Expression expression = parser.parseExpression(preAuthorize(method));
            return Boolean.TRUE.equals(expression.getValue(context, Boolean.class));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    // 只暴露表达式根所需能力；PermissionEvaluator 这里用不到但构造器要求，给一个恒定拒绝的实现
    private static final PermissionEvaluator DENY_ALL = new PermissionEvaluator() {

        @Override
        public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
            return false;
        }

        @Override
        public boolean hasPermission(Authentication authentication, java.io.Serializable targetId,
                                     String targetType, Object permission) {
            return false;
        }
    };

    private final class TestRoot extends SecurityExpressionRoot {

        TestRoot(Authentication authentication) {
            super(authentication);
            setRoleHierarchy(roleHierarchy);
            setPermissionEvaluator(DENY_ALL);
            // isAuthenticated()/hasAuthority 依赖 TrustResolver，不设置会在求值时 NPE
            setTrustResolver(new AuthenticationTrustResolverImpl());
        }
    }

    // 各端点真实的参数签名，供反射取 Method；写错会立刻在测试里报出来
    private static final java.util.Map<String, Method> VIEW_ENDPOINTS = java.util.Map.of(
            "pending", method("pending", com.starrainnotes.review.dto.ReviewQueryDTO.class),
            "history", method("history", com.starrainnotes.review.dto.ReviewHistoryQueryDTO.class),
            "detail", method("detail", Long.class),
            "cancel", method("cancel", Long.class, Long.class),
            "demoSubmit", method("demoSubmit", com.starrainnotes.review.dto.ReviewDemoSubmissionDTO.class,
                    Long.class));

    @ParameterizedTest
    @CsvSource({
            "pending,   review:read",
            "history,   review:history-read",
            "detail,    review:read",
            "demoSubmit, review:read",
    })
    @DisplayName("查看类端点：ADMIN 与 SUPER_ADMIN 都能通过（权限同时授予两个角色），普通用户被拒")
    void viewEndpointsAllowBothAdminRoles(String endpoint, String permission) {
        Method target = VIEW_ENDPOINTS.get(endpoint);

        assertThat(allowed(target, ADMIN, permission))
                .as("%s 应允许 ADMIN", endpoint).isTrue();
        assertThat(allowed(target, SUPER_ADMIN, permission))
                .as("%s 应允许 SUPER_ADMIN", endpoint).isTrue();
        // 普通用户连权限码都没有：这里刻意不传 permission，
        // 否则就等于在测「有权限就通过」，证明不了拒绝
        assertThat(allowed(target, "ROLE_USER"))
                .as("%s 不应允许普通用户", endpoint).isFalse();
    }

    @Test
    @DisplayName("REV-006 取消端点只要求已认证，身份必须等于申请人这条规则由 Service 对象级授权兜住")
    void cancelRequiresAuthenticatedActorOnly() {
        Method cancel = VIEW_ENDPOINTS.get("cancel");

        // URL/方法层放行「任何已登录账户」，因为「这条请求是不是他自己的」只有拿到数据才知道；
        // 真正的判定在 ReviewSubmissionService.cancelByApplicant 里（applicantAccountId 比对）
        assertThat(preAuthorize(cancel)).isEqualTo("isAuthenticated()");
        assertThat(allowed(cancel, "ROLE_USER")).isTrue();
        assertThat(allowed(cancel, ADMIN)).isTrue();
    }

    @Test
    @DisplayName("REV-002/003 缺少 review:read 的账户访问待审列表与详情一律被拒")
    void viewEndpointsDeniedWithoutReadPermission() {
        Method pending = method("pending", com.starrainnotes.review.dto.ReviewQueryDTO.class);
        Method detail = method("detail", Long.class);

        assertThat(allowed(pending, ADMIN)).isFalse();
        assertThat(allowed(pending, SUPER_ADMIN, "media:read")).isFalse();
        assertThat(allowed(detail, ADMIN)).isFalse();
        assertThat(allowed(detail, "review:history-read")).isFalse();
    }

    @Test
    @DisplayName("REV-007 缺少 review:history-read 的账户不能查询审核历史")
    void historyDeniedWithoutHistoryReadPermission() {
        Method history = method("history", com.starrainnotes.review.dto.ReviewHistoryQueryDTO.class);

        assertThat(allowed(history, ADMIN, "review:read")).isFalse();
        assertThat(allowed(history, ADMIN, "review:history-read")).isTrue();
    }

    @Test
    @DisplayName("REV-004 普通 ADMIN 没有 review:approve，审核通过必须被拒；只有 SUPER_ADMIN 通过")
    void approveRequiresSuperAdmin() {
        Method approve = method("approve", Long.class,
                com.starrainnotes.review.dto.ReviewApproveCommand.class, Long.class);

        assertThat(allowed(approve, ADMIN)).isFalse();
        assertThat(allowed(approve, ADMIN, "review:read")).isFalse();
        assertThat(allowed(approve, SUPER_ADMIN)).isFalse();
        assertThat(allowed(approve, SUPER_ADMIN, "review:approve")).isTrue();
    }

    @Test
    @DisplayName("REV-005 普通 ADMIN 没有 review:reject，审核拒绝必须被拒；只有 SUPER_ADMIN 通过")
    void rejectRequiresSuperAdmin() {
        Method reject = method("reject", Long.class,
                com.starrainnotes.review.dto.ReviewRejectCommand.class, Long.class);

        assertThat(allowed(reject, ADMIN)).isFalse();
        assertThat(allowed(reject, ADMIN, "review:read")).isFalse();
        assertThat(allowed(reject, SUPER_ADMIN)).isFalse();
        assertThat(allowed(reject, SUPER_ADMIN, "review:reject")).isTrue();
    }

    @Test
    @DisplayName("每个后台端点都显式声明了授权，没有端点只依赖 URL 层的 authenticated")
    void everyEndpointDeclaresAuthorization() {
        List<String> guarded = Arrays.stream(ReviewAdminController.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(org.springframework.web.bind.annotation.GetMapping.class)
                        || m.isAnnotationPresent(org.springframework.web.bind.annotation.PostMapping.class))
                .filter(m -> m.getAnnotation(PreAuthorize.class) == null)
                .map(Method::getName)
                .toList();

        assertThat(guarded).as("以下端点缺少 @PreAuthorize，会退化成只要登录即可访问").isEmpty();
    }
}
