package com.starrainnotes.account.controller;

import com.starrainnotes.account.dto.RegisterDTO;
import com.starrainnotes.account.dto.RegistrationCodeRequestDTO;
import com.starrainnotes.account.vo.EmailVerificationCodeVO;
import com.starrainnotes.account.service.EmailVerificationService;
import com.starrainnotes.account.service.AccountAuditService;
import com.starrainnotes.common.ApiException;
import com.starrainnotes.account.dto.LoginDTO;
import com.starrainnotes.account.dto.PasswordResetRequestDTO;
import com.starrainnotes.account.dto.PasswordResetConfirmDTO;
import com.starrainnotes.account.vo.CurrentAccountVO;
import com.starrainnotes.account.service.AccountAuthService;
import com.starrainnotes.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AccountAuthService service;
    private final EmailVerificationService verification;
    private final AccountAuditService audit;

    public AuthController(AccountAuthService service, EmailVerificationService verification,
                          AccountAuditService audit) {
        this.service = service;
        this.verification = verification;
        this.audit = audit;
    }

    @GetMapping("/csrf")
    public ApiResponse<Map<String, String>> csrf(CsrfToken token) {
        return ApiResponse.ok(Map.of("headerName", token.getHeaderName(), "token", token.getToken()));
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CurrentAccountVO> register(@Valid @RequestBody RegisterDTO request) {
        return ApiResponse.ok(service.register(request));
    }

    @PostMapping("/register/verification-codes")
    public ApiResponse<EmailVerificationCodeVO> sendEmailVerificationCode(
            @Valid @RequestBody RegistrationCodeRequestDTO request, HttpServletRequest servletRequest) {
        try {
            return ApiResponse.ok(verification.send(request.email(), servletRequest.getRemoteAddr()));
        } catch (ApiException exception) {
            if ("MAIL_DELIVERY_FAILED".equals(exception.getCode())) {
                audit.failed(null, null, "EMAIL_CODE_DELIVERY_FAILED");
            }
            throw exception;
        }
    }

    @PostMapping("/login")
    public ApiResponse<CurrentAccountVO> login(@Valid @RequestBody LoginDTO request,
                                          HttpServletRequest servletRequest,
                                          HttpServletResponse servletResponse) {
        return ApiResponse.ok(service.login(request, servletRequest, servletResponse));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        service.logout(request);
        return ApiResponse.ok(null);
    }

    @PostMapping("/password-reset/request")
    public ApiResponse<Void> requestReset(@Valid @RequestBody PasswordResetRequestDTO request,
                                          HttpServletRequest servletRequest) {
        service.requestPasswordReset(request, servletRequest);
        return ApiResponse.ok(null);
    }

    @PostMapping("/password-reset/confirm")
    public ApiResponse<Void> confirmReset(@Valid @RequestBody PasswordResetConfirmDTO request) {
        service.confirmPasswordReset(request);
        return ApiResponse.ok(null);
    }
}

