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
public interface AdminAccountService {

    PageResult<AdminAccountVO> accounts(int page, int pageSize, String keyword, String status);

    void changeStatus(String targetId, AccountStatusDTO request);

    AdminInvitationVO createInvitation(CreateAdminInvitationDTO request);

    void acceptInvitation(AcceptAdminInvitationDTO request);

    void acceptInvitationById(String invitationId);

    PageResult<InvitationRecordVO> invitations(int page, int pageSize);

    List<InvitationRecordVO> myInvitations();

    AdminInvitationVO resendInvitation(String invitationId);

    void revokeInvitation(String invitationId);

    void removeAdministrator(String targetId);

    PageResult<AccountAuditVO> audits(Long actor, Long target, String action, String result,
                                      LocalDateTime start, LocalDateTime end, int page, int pageSize);
}
