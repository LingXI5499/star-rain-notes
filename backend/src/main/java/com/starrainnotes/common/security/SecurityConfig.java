package com.starrainnotes.common.security;

import com.starrainnotes.auth.security.AdminUserDetailsService;
import org.apache.tomcat.util.http.Rfc6265CookieProcessor;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * V1 frozen authentication design (04-api-design.md §7, AGENTS.md):
 * server-side session, HttpOnly cookie, CSRF enabled, no JWT.
 *
 * <ul>
 *   <li>CSRF: cookie {@code XSRF-TOKEN} (readable by the SPA) echoed in header
 *       {@code X-XSRF-TOKEN}; plain (non-XOR) request handler so the SPA can
 *       send the raw cookie value.</li>
 *   <li>JSON login is performed in {@code AuthController} through the
 *       {@link AuthenticationManager}; the resulting SecurityContext is
 *       persisted to the HttpSession via the security context repository.</li>
 *   <li>401 / 403 / CSRF failures are rendered as RFC 9457 Problem Details.</li>
 *   <li>Session inactivity timeout ~8h and cookie attributes are configured in
 *       application.yml ({@code server.servlet.session.*}).</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * SameSite=Lax for every cookie (04-api-design.md §7), including the
     * JSESSIONID session cookie which Tomcat serializes internally through the
     * CookieProcessor (a response-wrapper filter cannot intercept it).
     */
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> sameSiteCookieCustomizer() {
        return factory -> factory.addContextCustomizers(context -> {
            Rfc6265CookieProcessor processor = new Rfc6265CookieProcessor();
            processor.setSameSiteCookies("lax");
            context.setCookieProcessor(processor);
        });
    }

    @Bean
    public HttpSessionSecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public AuthenticationManager authenticationManager(AdminUserDetailsService userDetailsService,
                                                      PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        // Cookie XSRF-TOKEN (non-HttpOnly so the SPA can read it), echoed in
        // the X-XSRF-TOKEN header. Also injected into AuthController for the
        // CSRF rotation after login/logout/password change.
        return CookieCsrfTokenRepository.withHttpOnlyFalse();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   RestAuthenticationEntryPoint authenticationEntryPoint,
                                                   RestAccessDeniedHandler accessDeniedHandler,
                                                   HttpSessionSecurityContextRepository securityContextRepository,
                                                   CsrfTokenRepository csrfTokenRepository)
            throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .securityContext(context -> context
                        .securityContextRepository(securityContextRepository))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/error",
                                // Public media: storage_path is a relative key and
                                // media URLs are part of published public content.
                                "/uploads/**").permitAll()
                        .requestMatchers("/api/v1/setup/**").permitAll()
                        .requestMatchers("/api/v1/auth/csrf",
                                "/api/v1/auth/session",
                                "/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/auth/super-admin-activation/**",
                                "/api/v1/auth/invitations/**",
                                "/api/v1/auth/password-reset/**",
                                "/api/v1/auth/account/login").permitAll()
                        .requestMatchers("/api/v1/public/**").permitAll()
                        .requestMatchers("/api/v1/super-admin/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/v1/admin/portfolio/**",
                                "/api/v1/admin/about/**",
                                "/api/v1/admin/site-settings/**",
                                "/api/v1/admin/english/analytics/**").hasRole("SUPER_ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/v1/admin/**/publish", "POST"),
                                new AntPathRequestMatcher("/api/v1/admin/**/withdraw", "POST"),
                                new AntPathRequestMatcher("/api/v1/admin/**", "DELETE"))
                        .hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/v1/auth/**", "/api/v1/admin/**").authenticated()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(Customizer.withDefaults()));
        return http.build();
    }
}
