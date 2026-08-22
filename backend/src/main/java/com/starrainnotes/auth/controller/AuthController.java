package com.starrainnotes.auth.controller;

import com.starrainnotes.auth.dto.AuthSessionView;
import com.starrainnotes.auth.dto.ChangePasswordRequest;
import com.starrainnotes.auth.dto.CsrfView;
import com.starrainnotes.auth.dto.LoginRequest;
import com.starrainnotes.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * V1 session authentication endpoints (04-api-design.md §7).
 *
 * <p>JSON login authenticates through the Spring Security
 * {@link AuthenticationManager} and persists the SecurityContext into the
 * HttpSession, so the next request is authenticated. The CSRF token is rotated
 * after login, logout and password change — the SPA re-fetches it via
 * GET /api/v1/auth/csrf.</p>
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final HttpSessionSecurityContextRepository securityContextRepository;
    private final CsrfTokenRepository csrfTokenRepository;
    private final AuthService authService;

    public AuthController(AuthenticationManager authenticationManager,
                          HttpSessionSecurityContextRepository securityContextRepository,
                          CsrfTokenRepository csrfTokenRepository,
                          AuthService authService) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.csrfTokenRepository = csrfTokenRepository;
        this.authService = authService;
    }

    @GetMapping("/csrf")
    public CsrfView csrf(HttpServletRequest request, HttpServletResponse response) {
        CsrfToken token = csrfTokenRepository.loadToken(request);
        if (token == null) {
            token = csrfTokenRepository.generateToken(request);
            csrfTokenRepository.saveToken(token, request, response);
        }
        return new CsrfView(token.getToken());
    }

    @GetMapping("/session")
    public AuthSessionView session() {
        return authService.currentSessionView();
    }

    @PostMapping("/login")
    public AuthSessionView login(@Valid @RequestBody LoginRequest loginRequest,
                                 HttpServletRequest httpRequest,
                                 HttpServletResponse httpResponse) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        loginRequest.username(), loginRequest.password()));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);
        httpRequest.changeSessionId();
        authService.recordLastLogin(authentication.getName());
        rotateCsrf(httpRequest, httpResponse);
        return authService.currentSessionView();
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        authService.logout(httpRequest);
        rotateCsrf(httpRequest, httpResponse);
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest changeRequest,
                               HttpServletRequest httpRequest,
                               HttpServletResponse httpResponse) {
        authService.changePassword(changeRequest.currentPassword(), changeRequest.newPassword(), httpRequest);
        rotateCsrf(httpRequest, httpResponse);
    }

    private void rotateCsrf(HttpServletRequest request, HttpServletResponse response) {
        CsrfToken token = csrfTokenRepository.generateToken(request);
        csrfTokenRepository.saveToken(token, request, response);
    }
}
