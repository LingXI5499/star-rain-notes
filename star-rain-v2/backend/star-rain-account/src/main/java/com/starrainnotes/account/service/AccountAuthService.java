package com.starrainnotes.account.service;

import com.starrainnotes.account.dto.ChangePasswordDTO;
import com.starrainnotes.account.dto.ConfirmEmailDTO;
import com.starrainnotes.account.dto.LoginDTO;
import com.starrainnotes.account.dto.PasswordResetConfirmDTO;
import com.starrainnotes.account.dto.PasswordResetRequestDTO;
import com.starrainnotes.account.dto.RegisterDTO;
import com.starrainnotes.account.dto.UpdateMyAccountDTO;
import com.starrainnotes.account.vo.CurrentAccountVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// 账户认证业务入口：注册、登录、登出、当前账户维护与密码找回。
public interface AccountAuthService {

    CurrentAccountVO register(RegisterDTO request);

    CurrentAccountVO login(LoginDTO request, HttpServletRequest servletRequest,
                           HttpServletResponse servletResponse);

    CurrentAccountVO confirmEmail(ConfirmEmailDTO request);

    void logout(HttpServletRequest request);

    CurrentAccountVO updateMe(UpdateMyAccountDTO request);

    void changePassword(ChangePasswordDTO request);

    void requestPasswordReset(PasswordResetRequestDTO request, HttpServletRequest servletRequest);

    void confirmPasswordReset(PasswordResetConfirmDTO request);

    void bootstrapSuperAdmin(boolean enabled, String username, String email, String password);
}
