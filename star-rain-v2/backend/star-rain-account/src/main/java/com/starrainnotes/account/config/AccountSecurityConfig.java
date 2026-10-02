package com.starrainnotes.account.config;

import com.starrainnotes.account.security.AccountAuthenticator;
import com.starrainnotes.account.security.RegistrationEntryRequestMatcher;
import com.starrainnotes.account.interceptor.AccountSessionValidationFilter;
import jakarta.servlet.http.HttpServletResponse;
import com.starrainnotes.account.mapper.AccountMapper;
import com.starrainnotes.common.context.HostEntryResolver;
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
 * 三入口隔离也落在这条链上：认证 API 只有一套，但注册类端点只对用户站开放，
 * 由 RegistrationEntryRequestMatcher 按 Host 判定（见下方规则顺序说明）。
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
                                            HostEntryResolver hostEntryResolver,
                                            List<ModuleSecurityContributor> contributors) throws Exception {
        HttpSessionCsrfTokenRepository csrfRepository = new HttpSessionCsrfTokenRepository();
        csrfRepository.setHeaderName("X-XSRF-TOKEN");
        // 按 order() 排序后，基础模块的具体规则会先于业务模块的宽泛规则声明
        List<ModuleSecurityContributor> ordered = contributors.stream()
                .sorted(Comparator.comparingInt(ModuleSecurityContributor::order))
                .toList();
        // 同一条入口规则既用于授权判定，也用于决定「拒绝时回 401 还是 403」，因此只构造一份
        RegistrationEntryRequestMatcher registrationEntryMatcher =
                new RegistrationEntryRequestMatcher(hostEntryResolver);
        http
                .csrf(csrf -> csrf.csrfTokenRepository(csrfRepository)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .securityContext(context -> context.securityContextRepository(contextRepository))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(authorize -> {
                    // 三段顺序即规则优先级：denied 先于 public，public 先于 authenticated
                    for (ModuleSecurityContributor contributor : ordered) {
                        if (!contributor.deniedPatterns().isEmpty()) {
                            authorize.requestMatchers(contributor.deniedPatterns().toArray(String[]::new)).denyAll();
                        }
                    }
                    /*
                     * 入口收窄：注册类端点只在用户站开放。
                     *
                     * 必须放在下面那批 permitAll 之前——/api/auth/register 也在 publicPatterns 里，
                     * 顺序反了就等于没写这条规则。
                     * 判定依赖 Host，而 requestMatchers 不支持按 Host 匹配，所以用自定义 RequestMatcher。
                     */
                    authorize.requestMatchers(registrationEntryMatcher).denyAll();
                    for (ModuleSecurityContributor contributor : ordered) {
                        if (!contributor.publicPatterns().isEmpty()) {
                            authorize.requestMatchers(contributor.publicPatterns().toArray(String[]::new)).permitAll();
                        }
                    }
                    for (ModuleSecurityContributor contributor : ordered) {
                        if (!contributor.authenticatedPatterns().isEmpty()) {
                            authorize.requestMatchers(contributor.authenticatedPatterns().toArray(String[]::new))
                                    .authenticated();
                        }
                    }
                    // 默认拒绝：没有声明过的 URL 一律不可访问
                    authorize.anyRequest().denyAll();
                })
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, failure) -> {
                            /*
                             * 匿名请求命中入口收窄规则时必须回 403，不能回 401。
                             *
                             * ExceptionTranslationFilter 对「匿名 + 授权被拒」的默认处理是发起认证挑战，
                             * 但那对这里恰好是错的答案：注册在公开站与管理站是本就不存在的能力，
                             * 不是登录态问题。回 401 会误导调用方去登录，而且在公开站根本没有登录入口。
                             * 其它 URL 的 401「请先登录」语义完全不变。
                             */
                            if (registrationEntryMatcher.matches(request)) {
                                writeSecurityError(response, 403, "FORBIDDEN", "没有操作权限");
                                return;
                            }
                            writeSecurityError(response, 401, "UNAUTHORIZED", "请先登录");
                        })
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
