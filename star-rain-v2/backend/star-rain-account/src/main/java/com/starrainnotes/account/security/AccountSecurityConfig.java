package com.starrainnotes.account.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import com.starrainnotes.account.mapper.AccountMapper;
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
                                            AccountMapper mapper) throws Exception {
        HttpSessionCsrfTokenRepository csrfRepository = new HttpSessionCsrfTokenRepository();
        csrfRepository.setHeaderName("X-XSRF-TOKEN");
        http
                .csrf(csrf -> csrf.csrfTokenRepository(csrfRepository)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .securityContext(context -> context.securityContextRepository(contextRepository))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/admin/roles", "/api/admin/roles/**", "/api/admin/permissions",
                                "/api/admin/accounts/*/roles").denyAll()
                        .requestMatchers("/api/auth/register", "/api/auth/login",
                                "/api/auth/register/verification-codes",
                                "/api/auth/password-reset/**", "/api/auth/csrf").permitAll()
                        .requestMatchers("/api/auth/logout", "/api/account/**", "/api/admin/**")
                        .authenticated()
                        .anyRequest().denyAll())
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
