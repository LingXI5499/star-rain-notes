/*
 * AuthController：账号模块的HTTP 接入文件。
 * 业务范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 * 主要协作类型：RegisterDTO、RegistrationCodeRequestDTO、EmailVerificationCodeVO、EmailVerificationService、LoginDTO、PasswordResetRequestDTO、PasswordResetConfirmDTO。
 * 阅读入口：csrf（CSRF处理入口）；register（注册认证）；sendEmailVerificationCode（发送邮箱验证代码）；login（执行登录认证）。
 */

package com.starrainnotes.account.controller;

import com.starrainnotes.account.dto.RegisterDTO;
import com.starrainnotes.account.dto.RegistrationCodeRequestDTO;
import com.starrainnotes.account.vo.EmailVerificationCodeVO;
import com.starrainnotes.account.service.EmailVerificationService;
import com.starrainnotes.account.dto.LoginDTO;
import com.starrainnotes.account.dto.PasswordResetRequestDTO;
import com.starrainnotes.account.dto.PasswordResetConfirmDTO;
import com.starrainnotes.account.vo.CurrentAccountVO;
import com.starrainnotes.account.service.AccountAuthService;
import com.starrainnotes.common.result.ApiResponse;
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

/**
 * 认证控制器，属于账号模块。
 * 声明 HTTP 路由，接收路径、查询或请求体参数，并将业务结果包装为统一响应。权限和参数校验以本类注解及调用服务为准。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /**
     * 业务服务。
     * 协作类型：<code>AccountAuthService</code>。
     */
    private final AccountAuthService service;
    /**
     * 验证。
     * 协作类型：<code>EmailVerificationService</code>。
     */
    private final EmailVerificationService verification;

    /**
     * 构造AuthController，接收本类使用的依赖或初始化参数。
     *
     * @param service 业务服务；类型为 <code>AccountAuthService</code>
     * @param verification 验证；类型为 <code>EmailVerificationService</code>
     */
    public AuthController(AccountAuthService service, EmailVerificationService verification) {
        this.service = service;
        this.verification = verification;
    }

    /**
     * CSRF处理入口。
     * HTTP 映射：<code>@GetMapping("/csrf")</code>。
     * 关键处理步骤调用 <code>ApiResponse.ok</code>、<code>Map.of</code>、<code>token.getHeaderName</code>、<code>token.getToken</code>。
     *
     * @param token 令牌；类型为 <code>CsrfToken</code>
     * @return 类型为 <code>ApiResponse&lt;Map&lt;String, String&gt;&gt;</code> 的处理结果
     */
    @GetMapping("/csrf")
    public ApiResponse<Map<String, String>> csrf(CsrfToken token) {
        return ApiResponse.ok(Map.of("headerName", token.getHeaderName(), "token", token.getToken()));
    }

    /**
     * 注册认证。
     * HTTP 映射：<code>@PostMapping("/register")</code>。
     * 主要协作调用：<code>service.register</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>RegisterDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;CurrentAccountVO&gt;</code> 的处理结果
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CurrentAccountVO> register(@Valid @RequestBody RegisterDTO request) {
        return ApiResponse.ok(service.register(request));
    }

    /**
     * 发送邮箱验证代码。
     * HTTP 映射：<code>@PostMapping("/register/verification-codes")</code>。
     * 主要协作调用：<code>verification.send</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>RegistrationCodeRequestDTO</code>；嵌套字段继续执行校验
     * @param servletRequest servlet请求；类型为 <code>HttpServletRequest</code>
     * @return 类型为 <code>ApiResponse&lt;EmailVerificationCodeVO&gt;</code> 的处理结果
     */
    @PostMapping("/register/verification-codes")
    public ApiResponse<EmailVerificationCodeVO> sendEmailVerificationCode(
            @Valid @RequestBody RegistrationCodeRequestDTO request, HttpServletRequest servletRequest) {
        // 发信失败的审计由 EmailVerificationService 写到独立事务里，接入层不再拦异常。
        return ApiResponse.ok(verification.send(request.getEmail(), servletRequest.getRemoteAddr()));
    }

    /**
     * 执行登录认证。
     * HTTP 映射：<code>@PostMapping("/login")</code>。
     * 主要协作调用：<code>service.login</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>LoginDTO</code>；嵌套字段继续执行校验
     * @param servletRequest servlet请求；类型为 <code>HttpServletRequest</code>
     * @param servletResponse servlet响应；类型为 <code>HttpServletResponse</code>
     * @return 类型为 <code>ApiResponse&lt;CurrentAccountVO&gt;</code> 的处理结果
     */
    @PostMapping("/login")
    public ApiResponse<CurrentAccountVO> login(@Valid @RequestBody LoginDTO request,
                                          HttpServletRequest servletRequest,
                                          HttpServletResponse servletResponse) {
        return ApiResponse.ok(service.login(request, servletRequest, servletResponse));
    }

    /**
     * 执行退出登录认证。
     * HTTP 映射：<code>@PostMapping("/logout")</code>。
     * 主要协作调用：<code>service.logout</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>HttpServletRequest</code>
     * @return 类型为 <code>ApiResponse&lt;Void&gt;</code> 的处理结果
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        service.logout(request);
        return ApiResponse.ok(null);
    }

    /**
     * 请求Reset处理入口。
     * HTTP 映射：<code>@PostMapping("/password-reset/request")</code>。
     * 主要协作调用：<code>service.requestPasswordReset</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>PasswordResetRequestDTO</code>；嵌套字段继续执行校验
     * @param servletRequest servlet请求；类型为 <code>HttpServletRequest</code>
     * @return 类型为 <code>ApiResponse&lt;Void&gt;</code> 的处理结果
     */
    @PostMapping("/password-reset/request")
    public ApiResponse<Void> requestReset(@Valid @RequestBody PasswordResetRequestDTO request,
                                          HttpServletRequest servletRequest) {
        service.requestPasswordReset(request, servletRequest);
        return ApiResponse.ok(null);
    }

    /**
     * 确认Reset。
     * HTTP 映射：<code>@PostMapping("/password-reset/confirm")</code>。
     * 主要协作调用：<code>service.confirmPasswordReset</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>PasswordResetConfirmDTO</code>；嵌套字段继续执行校验
     * @return 类型为 <code>ApiResponse&lt;Void&gt;</code> 的处理结果
     */
    @PostMapping("/password-reset/confirm")
    public ApiResponse<Void> confirmReset(@Valid @RequestBody PasswordResetConfirmDTO request) {
        service.confirmPasswordReset(request);
        return ApiResponse.ok(null);
    }
}

