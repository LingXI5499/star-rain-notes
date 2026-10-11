/*
 * AccountAuthService：账号模块的业务契约文件。
 * 业务范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：ChangePasswordDTO、ConfirmEmailDTO、LoginDTO、PasswordResetConfirmDTO、PasswordResetRequestDTO、RegisterDTO、UpdateMyAccountDTO。
 * 阅读入口：register（注册账号认证）；login（执行登录账号认证）；confirmEmail（确认邮箱）；logout（执行退出登录账号认证）。
 */

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
/**
 * 账号认证业务服务，属于账号模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface AccountAuthService {

    /**
     * 注册账号认证。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>RegisterDTO</code>
     * @return 类型为 <code>CurrentAccountVO</code> 的处理结果
     */
    CurrentAccountVO register(RegisterDTO request);

    /**
     * 执行登录账号认证。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>LoginDTO</code>
     * @param servletRequest servlet请求；类型为 <code>HttpServletRequest</code>
     * @param servletResponse servlet响应；类型为 <code>HttpServletResponse</code>
     * @return 类型为 <code>CurrentAccountVO</code> 的处理结果
     */
    CurrentAccountVO login(LoginDTO request, HttpServletRequest servletRequest,
                           HttpServletResponse servletResponse);

    /**
     * 确认邮箱。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>ConfirmEmailDTO</code>
     * @return 类型为 <code>CurrentAccountVO</code> 的处理结果
     */
    CurrentAccountVO confirmEmail(ConfirmEmailDTO request);

    /**
     * 执行退出登录账号认证。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>HttpServletRequest</code>
     */
    void logout(HttpServletRequest request);

    /**
     * 更新Me。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>UpdateMyAccountDTO</code>
     * @return 类型为 <code>CurrentAccountVO</code> 的处理结果
     */
    CurrentAccountVO updateMe(UpdateMyAccountDTO request);

    /**
     * change密码处理入口。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>ChangePasswordDTO</code>
     */
    void changePassword(ChangePasswordDTO request);

    /**
     * 请求密码Reset处理入口。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>PasswordResetRequestDTO</code>
     * @param servletRequest servlet请求；类型为 <code>HttpServletRequest</code>
     */
    void requestPasswordReset(PasswordResetRequestDTO request, HttpServletRequest servletRequest);

    /**
     * 确认密码Reset。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>PasswordResetConfirmDTO</code>
     */
    void confirmPasswordReset(PasswordResetConfirmDTO request);

    /**
     * 初始化Super后台处理入口。
     *
     * @param enabled 已启用；类型为 <code>boolean</code>
     * @param username 用户名；类型为 <code>String</code>
     * @param email 邮箱；类型为 <code>String</code>
     * @param password 密码参数；使用和保存方式由对应的密码处理流程决定；类型为 <code>String</code>
     */
    void bootstrapSuperAdmin(boolean enabled, String username, String email, String password);
}
