/*
 * AdminAccountService：账号模块的业务契约文件。
 * 业务范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：AccountStatusDTO、AcceptAdminInvitationDTO、CreateAdminInvitationDTO、AccountAuditVO、AdminAccountVO、AdminInvitationVO、InvitationRecordVO。
 * 阅读入口：accounts（账号集合处理入口）；changeStatus（change状态处理入口）；createInvitation（创建邀请）；acceptInvitation（接受邀请）。
 */

package com.starrainnotes.account.service;

import com.starrainnotes.account.dto.AccountStatusDTO;
import com.starrainnotes.account.dto.AcceptAdminInvitationDTO;
import com.starrainnotes.account.dto.CreateAdminInvitationDTO;
import com.starrainnotes.account.vo.AccountAuditVO;
import com.starrainnotes.account.vo.AdminAccountVO;
import com.starrainnotes.account.vo.AdminInvitationVO;
import com.starrainnotes.account.vo.InvitationRecordVO;
import com.starrainnotes.common.result.PageResult;
import java.time.LocalDateTime;
import java.util.List;

// 管理端账户与管理员邀请业务入口。
/**
 * 后台账号业务服务，属于账号模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface AdminAccountService {

    /**
     * 账号集合处理入口。
     *
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @param keyword 关键词；类型为 <code>String</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>
     * @return 类型为 <code>PageResult&lt;AdminAccountVO&gt;</code> 的处理结果
     */
    PageResult<AdminAccountVO> accounts(int page, int pageSize, String keyword, String status);

    /**
     * change状态处理入口。
     *
     * @param targetId 被处理或审核的目标对象标识；类型为 <code>String</code>
     * @param request 本次操作的请求参数对象；类型为 <code>AccountStatusDTO</code>
     */
    void changeStatus(String targetId, AccountStatusDTO request);

    /**
     * 创建邀请。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>CreateAdminInvitationDTO</code>
     * @return 类型为 <code>AdminInvitationVO</code> 的处理结果
     */
    AdminInvitationVO createInvitation(CreateAdminInvitationDTO request);

    /**
     * 接受邀请。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>AcceptAdminInvitationDTO</code>
     */
    void acceptInvitation(AcceptAdminInvitationDTO request);

    /**
     * 接受邀请依据标识。
     *
     * @param invitationId 邀请标识；类型为 <code>String</code>
     */
    void acceptInvitationById(String invitationId);

    /**
     * invitations处理入口。
     *
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @return 类型为 <code>PageResult&lt;InvitationRecordVO&gt;</code> 的处理结果
     */
    PageResult<InvitationRecordVO> invitations(int page, int pageSize);

    /**
     * myInvitations处理入口。
     * @return 本次处理得到的条目集合
     */
    List<InvitationRecordVO> myInvitations();

    /**
     * resend邀请处理入口。
     *
     * @param invitationId 邀请标识；类型为 <code>String</code>
     * @return 类型为 <code>AdminInvitationVO</code> 的处理结果
     */
    AdminInvitationVO resendInvitation(String invitationId);

    /**
     * 撤销邀请。
     *
     * @param invitationId 邀请标识；类型为 <code>String</code>
     */
    void revokeInvitation(String invitationId);

    /**
     * 移除Administrator。
     *
     * @param targetId 被处理或审核的目标对象标识；类型为 <code>String</code>
     */
    void removeAdministrator(String targetId);

    /**
     * audits处理入口。
     *
     * @param actor 当前操作对应的身份对象；类型为 <code>Long</code>
     * @param target 目标；类型为 <code>Long</code>
     * @param action 动作；类型为 <code>String</code>
     * @param result 结果；类型为 <code>String</code>
     * @param start 开始；类型为 <code>LocalDateTime</code>
     * @param end 结束；类型为 <code>LocalDateTime</code>
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @return 类型为 <code>PageResult&lt;AccountAuditVO&gt;</code> 的处理结果
     */
    PageResult<AccountAuditVO> audits(Long actor, Long target, String action, String result,
                                      LocalDateTime start, LocalDateTime end, int page, int pageSize);
}
