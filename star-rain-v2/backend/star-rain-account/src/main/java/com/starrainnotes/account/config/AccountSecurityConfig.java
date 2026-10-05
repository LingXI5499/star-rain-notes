package com.starrainnotes.account.config;

import com.starrainnotes.account.security.AccountAuthenticator;
import com.starrainnotes.account.security.SecurityPatternValidator;
import com.starrainnotes.account.security.SecurityRulePlan;
import com.starrainnotes.account.interceptor.AccountSessionValidationFilter;
import jakarta.servlet.http.HttpServletResponse;
import com.starrainnotes.account.mapper.AccountMapper;
import com.starrainnotes.common.security.ModuleSecurityContributor;
import java.util.Comparator;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;

/*
 * V2 唯一的 Spring Security 过滤器链。
 *
 * CSRF、会话策略、会话有效性校验与统一 401/403 输出只在这里实现一次。
 * 各业务模块通过 ModuleSecurityContributor 声明自己的 URL 边界，
 * 因此业务模块既不重复这套配置，也不会绕过 AccountSessionValidationFilter 的账户停用校验。
 *
 * 入口隔离不再由这条链承担：入口已经从「按域名」改成「按路径」（同一个域名 + 公开树 / 账号树），
 * 服务端拿不到任何可用于区分两条路径树的请求特征——同一个域名下 / 与 /useradmin 的
 * 请求头、Cookie、方法完全一样，因此「公开站不暴露注册登录」退化为前端路由级保证。
 * 注册接口恢复为普通公开接口，其既有业务约束（邮箱验证码、参数校验、用户名唯一）不变。
 */
@Configuration
@EnableMethodSecurity
public class AccountSecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    AuthenticationManager authenticationManager(AccountAuthenticator authenticator) {
        return new ProviderManager(authenticator);
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            SecurityContextRepository contextRepository,
                                            AccountMapper mapper,
                                            List<ModuleSecurityContributor> contributors) throws Exception {
        HttpSessionCsrfTokenRepository csrfRepository = new HttpSessionCsrfTokenRepository();
        csrfRepository.setHeaderName("X-XSRF-TOKEN");
        // 按 order() 排序后，基础模块的具体规则会先于业务模块的宽泛规则声明
        List<ModuleSecurityContributor> ordered = contributors.stream()
                .sorted(Comparator.comparingInt(ModuleSecurityContributor::order))
                .toList();
        SecurityPatternValidator.validate(ordered);
        http
                .csrf(csrf -> csrf.csrfTokenRepository(csrfRepository)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .securityContext(context -> context.securityContextRepository(contextRepository))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(authorize -> {
                    /*
                     * 规则顺序即优先级，且这个顺序**不在这里决定**：
                     * SecurityRulePlan.of() 是唯一真源（denied → public → authenticated），
                     * SecurityRulePlanTest 把它钉死。曾经这里是三段并列的 for 循环，
                     * 谁先谁后只存在于阅读顺序里，而 Account 的
                     * denied `/api/admin/accounts/* /roles` 正是靠「denied 先跑」才成立。
                     */
                    for (SecurityRulePlan.Rule rule : SecurityRulePlan.of(ordered)) {
                        switch (rule.getAccess()) {
                            case DENIED -> authorize.requestMatchers(rule.getPattern()).denyAll();
                            case PUBLIC -> authorize.requestMatchers(rule.getPattern()).permitAll();
                            case AUTHENTICATED -> authorize.requestMatchers(rule.getPattern()).authenticated();
                        }
                    }
                    // 默认拒绝：没有声明过的 URL 一律不可访问
                    authorize.anyRequest().denyAll();
                })
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, failure) ->
                                writeSecurityError(response, 401, "UNAUTHORIZED", "请先登录"))
                        .accessDeniedHandler((request, response, failure) ->
                                writeSecurityError(response, 403,
                                        failure instanceof CsrfException ? "CSRF_INVALID" : "FORBIDDEN",
                                        failure instanceof CsrfException ? "安全凭据已过期，请重试" : "没有操作权限")))
                ;
        http.httpBasic(basic -> basic.disable());
        http.formLogin(form -> form.disable());
        // 会话有效性校验必须作用于全部模块的 URL，因此挂在唯一这条链上
        http.addFilterBefore(new AccountSessionValidationFilter(mapper), AuthorizationFilter.class);
        return http.build();
    }

    private static void writeSecurityError(HttpServletResponse response, int status,
                                           String code, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message
                + "\",\"data\":null}");
    }
}
