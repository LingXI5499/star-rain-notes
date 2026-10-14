package com.starrainnotes.boot.reliability;

import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/** A separate management servlet cannot reuse the main servlet's session; allow stateless local diagnostics. */
@Configuration
public class ManagementEndpointSecurity {
    @Bean
    @Order(0)
    public SecurityFilterChain managementEndpoints(HttpSecurity http) throws Exception {
        var loopback = Set.of("127.0.0.1", "::1", "0:0:0:0:0:0:0:1");
        return http.securityMatcher("/actuator/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .anyRequest().access((user, context) -> new AuthorizationDecision(
                                loopback.contains(context.getRequest().getRemoteAddr()) && user.get().getAuthorities().stream()
                                        .anyMatch(authority -> "site:dashboard-read".equals(authority.getAuthority())))))
                .build();
    }
}
