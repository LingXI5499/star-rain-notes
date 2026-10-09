package com.starrainnotes.account.mapper;

import com.starrainnotes.account.entity.AccountAuditEntity;
import com.starrainnotes.account.entity.AccountEntity;
import com.starrainnotes.account.entity.AdminInvitationEntity;
import com.starrainnotes.account.entity.PasswordResetEntity;
import com.starrainnotes.account.entity.PermissionEntity;
import com.starrainnotes.account.entity.RoleEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AccountMapper {

    AccountEntity accountByIdentifier(@Param("identifier") String identifier);
    AccountEntity accountById(@Param("id") long id);
    AccountEntity accountByIdForUpdate(@Param("id") long id);
    AccountEntity accountByEmail(@Param("email") String email);
    int usernameCount(@Param("username") String username);
    int emailCount(@Param("email") String email);
    void insertAccount(AccountEntity account);
    void insertCredential(@Param("accountId") long accountId, @Param("passwordHash") String passwordHash);
    String passwordHash(@Param("accountId") long accountId);
    int updatePassword(@Param("accountId") long accountId, @Param("passwordHash") String passwordHash);
    int updateDisplayName(@Param("accountId") long accountId, @Param("displayName") String displayName);
    int verifyEmail(@Param("accountId") long accountId);
    int updateLastLogin(@Param("accountId") long accountId);
    int incrementAuthVersion(@Param("accountId") long accountId);
    int incrementVersionsForRole(@Param("roleId") long roleId);
    int updateStatus(@Param("accountId") long accountId, @Param("status") String status);

    RoleEntity roleByCode(@Param("code") String code);
    RoleEntity roleById(@Param("id") long id);
    List<RoleEntity> allRoles();
    List<PermissionEntity> allPermissions();
    List<String> roleCodes(@Param("accountId") long accountId);
    List<String> permissionCodes(@Param("accountId") long accountId);
    List<Long> roleIds(@Param("accountId") long accountId);
    List<Long> permissionIdsForRole(@Param("roleId") long roleId);
    int activeSuperAdminCount();
    int superAdminAssignmentCount();
    Long lockSuperAdminRole();
    void insertAccountRole(@Param("accountId") long accountId, @Param("roleId") long roleId, @Param("grantedBy") Long grantedBy);
    int deleteAccountRoles(@Param("accountId") long accountId);
    int deleteAccountRole(@Param("accountId") long accountId, @Param("roleId") long roleId);
    int deleteRolePermissions(@Param("roleId") long roleId);
    void insertRolePermission(@Param("roleId") long roleId, @Param("permissionId") long permissionId);
    int permissionCount(@Param("id") long id);
    int rolePermissionCount(@Param("roleId") long roleId, @Param("permissionId") long permissionId);

    long accountPageCount(@Param("keyword") String keyword, @Param("status") String status);
    List<AccountEntity> accountPage(@Param("keyword") String keyword, @Param("status") String status,
                                          @Param("offset") int offset, @Param("limit") int limit);

    void insertInvitation(AdminInvitationEntity invitation);
    AdminInvitationEntity invitationById(@Param("id") long id);
    AdminInvitationEntity invitationByIdForUpdate(@Param("id") long id);
    AdminInvitationEntity invitationByHash(@Param("hash") String hash);
    List<AdminInvitationEntity> invitationPage(@Param("offset") int offset, @Param("limit") int limit);
    long invitationPageCount();
    List<AdminInvitationEntity> pendingInvitationsForAccount(@Param("accountId") long accountId);
    void saveInvitationDelivery(AdminInvitationEntity invitation);
    void revokeInvitation(@Param("id") long id);
    AdminInvitationEntity invitationByHashForUpdate(@Param("hash") String hash);
    int pendingInvitationCount(@Param("targetId") long targetId);
    int acceptInvitation(@Param("id") long id, @Param("accountId") long accountId);
    int revokePendingInvitations(@Param("targetId") long targetId);

    void insertReset(PasswordResetEntity reset);
    PasswordResetEntity resetByHashForUpdate(@Param("hash") String hash);
    int useReset(@Param("id") long id);
    int expirePendingResets(@Param("accountId") long accountId);
    int recentResetRequestsByIp(@Param("ip") String ip, @Param("since") LocalDateTime since);
    int recentAuditCount(@Param("ip") String ip, @Param("action") String action,
                         @Param("since") LocalDateTime since);

    void insertAudit(AccountAuditEntity audit);
    long auditPageCount(@Param("actor") Long actor, @Param("target") Long target,
                        @Param("action") String action, @Param("result") String result,
                        @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    List<AccountAuditEntity> auditPage(@Param("actor") Long actor, @Param("target") Long target,
                                      @Param("action") String action, @Param("result") String result,
                                      @Param("start") LocalDateTime start, @Param("end") LocalDateTime end,
                                      @Param("offset") int offset, @Param("limit") int limit);
}
