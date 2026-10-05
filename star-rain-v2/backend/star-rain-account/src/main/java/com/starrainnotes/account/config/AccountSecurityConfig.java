package com.starrainnotes.account.config;

import com.starrainnotes.account.security.AccountAuthenticator;
import com.starrainnotes.account.security.SecurityPatternValidator;
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
                    // 三段顺序即规则优先级：denied 先于 public，public 先于 authenticated
                    for (ModuleSecurityContributor contributor : ordered) {
                        if (!contributor.deniedPatterns().isEmpty()) {
                            authorize.requestMatchers(contributor.deniedPatterns().toArray(String[]::new)).denyAll();
                        }
                    }
                    /*
                     * 入口收窄规则已随「三入口按域名」方案一并移除。
                     *
                     * 路径方案下注册接口是普通公开接口：同一个域名上无法从请求里分辨
                     * 调用方来自公开树还是账号树。少了这一层的后果与补偿见
                     * docs/开发文档/路径入口返工验收.md 的「已知缺口」。
                     */
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
