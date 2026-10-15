/*
 * AccountMapper：账号模块的数据访问文件。
 * 业务范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：AccountAuditEntity、AccountEntity、AdminInvitationEntity、PasswordResetEntity、PermissionEntity、RoleEntity。
 * 阅读入口：accountByIdentifier（按Identifier读取账号）；accountById（按标识读取账号）；accountByIdForUpdate（按标识加行锁读取账号）；accountByEmail（按邮箱读取账号）。
 */

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

/**
 * 账号数据映射，属于账号模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
@Mapper
public interface AccountMapper {

    /**
     * 按Identifier读取账号。
     *
     * 持久化操作：读取，涉及 <code>sr_account</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param identifier identifier；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("identifier")</code>
     * @return 类型为 <code>AccountEntity</code> 的处理结果
     */
    AccountEntity accountByIdentifier(@Param("identifier") String identifier);
    /**
     * 按标识读取账号。
     *
     * 持久化操作：读取，涉及 <code>sr_account</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>AccountEntity</code> 的处理结果
     */
    AccountEntity accountById(@Param("id") long id);
    /**
     * 按标识加行锁读取账号。
     *
     * 持久化操作：读取，涉及 <code>sr_account</code>。
     * 此查询使用行锁；调用方应在事务中完成后续状态检查和写入。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>AccountEntity</code> 的处理结果
     */
    AccountEntity accountByIdForUpdate(@Param("id") long id);
    /**
     * 按邮箱读取账号。
     *
     * 持久化操作：读取，涉及 <code>sr_account</code>。
     *
     * @param email 邮箱；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("email")</code>
     * @return 类型为 <code>AccountEntity</code> 的处理结果
     */
    AccountEntity accountByEmail(@Param("email") String email);
    /**
     * 用户名数量处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_account</code>。
     *
     * @param username 用户名；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("username")</code>
     * @return 符合 SQL 条件的记录数量
     */
    int usernameCount(@Param("username") String username);
    /**
     * 邮箱数量处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_account</code>。
     *
     * @param email 邮箱；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("email")</code>
     * @return 符合 SQL 条件的记录数量
     */
    int emailCount(@Param("email") String email);
    /**
     * 写入账号。
     *
     * 持久化操作：新增，涉及 <code>sr_account</code>。
     *
     * @param account 账号；类型为 <code>AccountEntity</code>
     */
    void insertAccount(AccountEntity account);
    /**
     * 写入Credential。
     *
     * 持久化操作：新增，涉及 <code>sr_account_credential</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param passwordHash 密码的哈希值，不能作为明文密码或公开资料返回；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("passwordHash")</code>
     */
    void insertCredential(@Param("accountId") long accountId, @Param("passwordHash") String passwordHash);
    /**
     * 密码的哈希值，不能作为明文密码或公开资料返回处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_account_credential</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    String passwordHash(@Param("accountId") long accountId);
    /**
     * 更新密码。
     *
     * 持久化操作：更新，涉及 <code>sr_account_credential</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param passwordHash 密码的哈希值，不能作为明文密码或公开资料返回；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("passwordHash")</code>
     * @return SQL 写入影响的记录行数
     */
    int updatePassword(@Param("accountId") long accountId, @Param("passwordHash") String passwordHash);
    /**
     * 更新显示名称。
     *
     * 持久化操作：更新，涉及 <code>sr_account</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param displayName 显示名称；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("displayName")</code>
     * @return SQL 写入影响的记录行数
     */
    int updateDisplayName(@Param("accountId") long accountId, @Param("displayName") String displayName);
    /**
     * 验证邮箱。
     *
     * 持久化操作：更新，涉及 <code>sr_account</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @return SQL 写入影响的记录行数
     */
    int verifyEmail(@Param("accountId") long accountId);
    /**
     * 更新最近Login。
     *
     * 持久化操作：更新，涉及 <code>sr_account</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @return SQL 写入影响的记录行数
     */
    int updateLastLogin(@Param("accountId") long accountId);
    /**
     * increment认证版本处理入口。
     *
     * 持久化操作：更新，涉及 <code>sr_account</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @return SQL 写入影响的记录行数
     */
    int incrementAuthVersion(@Param("accountId") long accountId);
    /**
     * incrementVersions面向角色处理入口。
     *
     * 持久化操作：更新，涉及 <code>sr_account</code>、<code>sr_account_role</code>。
     *
     * @param roleId 角色标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("roleId")</code>
     * @return SQL 写入影响的记录行数
     */
    int incrementVersionsForRole(@Param("roleId") long roleId);
    /**
     * 更新状态。
     *
     * 持久化操作：更新，涉及 <code>sr_account</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("status")</code>
     * @return SQL 写入影响的记录行数
     */
    int updateStatus(@Param("accountId") long accountId, @Param("status") String status);

    /**
     * 按代码读取角色。
     *
     * 持久化操作：读取，涉及 <code>sr_role</code>。
     *
     * @param code 代码；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("code")</code>
     * @return 类型为 <code>RoleEntity</code> 的处理结果
     */
    RoleEntity roleByCode(@Param("code") String code);
    /**
     * 按标识读取角色。
     *
     * 持久化操作：读取，涉及 <code>sr_role</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>RoleEntity</code> 的处理结果
     */
    RoleEntity roleById(@Param("id") long id);
    /**
     * 全部角色集合处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_role</code>。
     * @return 本次处理得到的条目集合
     */
    List<RoleEntity> allRoles();
    /**
     * 全部权限集合处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_permission</code>。
     * @return 本次处理得到的条目集合
     */
    List<PermissionEntity> allPermissions();
    /**
     * 角色Codes处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_account_role</code>、<code>sr_role</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @return 本次处理得到的条目集合
     */
    List<String> roleCodes(@Param("accountId") long accountId);
    /**
     * 权限Codes处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_account_role</code>、<code>sr_role</code>、<code>sr_role_permission</code>、<code>sr_permission</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @return 本次处理得到的条目集合
     */
    List<String> permissionCodes(@Param("accountId") long accountId);
    /**
     * 角色标识集合处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_account_role</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @return 本次处理得到的条目集合
     */
    List<Long> roleIds(@Param("accountId") long accountId);
    /**
     * 权限标识集合面向角色处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_role_permission</code>。
     *
     * @param roleId 角色标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("roleId")</code>
     * @return 本次处理得到的条目集合
     */
    List<Long> permissionIdsForRole(@Param("roleId") long roleId);
    /**
     * 有效Super后台数量处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_account_role</code>、<code>sr_role</code>、<code>sr_account</code>。
     * @return 符合 SQL 条件的记录数量
     */
    int activeSuperAdminCount();
    /**
     * super后台Assignment数量处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_account_role</code>、<code>sr_role</code>。
     * @return 符合 SQL 条件的记录数量
     */
    int superAdminAssignmentCount();
    /**
     * 锁Super后台角色处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_role</code>。
     * 此查询使用行锁；调用方应在事务中完成后续状态检查和写入。
     * @return 类型为 <code>Long</code> 的处理结果
     */
    Long lockSuperAdminRole();
    /**
     * 写入账号角色。
     *
     * 持久化操作：新增，涉及 <code>sr_account_role</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param roleId 角色标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("roleId")</code>
     * @param grantedBy 已授权依据；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("grantedBy")</code>
     */
    void insertAccountRole(@Param("accountId") long accountId, @Param("roleId") long roleId, @Param("grantedBy") Long grantedBy);
    /**
     * 删除账号角色集合。
     *
     * 持久化操作：删除，涉及 <code>sr_account_role</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @return SQL 写入影响的记录行数
     */
    int deleteAccountRoles(@Param("accountId") long accountId);
    /**
     * 删除账号角色。
     *
     * 持久化操作：删除，涉及 <code>sr_account_role</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @param roleId 角色标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("roleId")</code>
     * @return SQL 写入影响的记录行数
     */
    int deleteAccountRole(@Param("accountId") long accountId, @Param("roleId") long roleId);
    /**
     * 删除角色权限集合。
     *
     * 持久化操作：删除，涉及 <code>sr_role_permission</code>。
     *
     * @param roleId 角色标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("roleId")</code>
     * @return SQL 写入影响的记录行数
     */
    int deleteRolePermissions(@Param("roleId") long roleId);
    /**
     * 写入角色权限。
     *
     * 持久化操作：新增，涉及 <code>sr_role_permission</code>。
     *
     * @param roleId 角色标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("roleId")</code>
     * @param permissionId 权限标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("permissionId")</code>
     */
    void insertRolePermission(@Param("roleId") long roleId, @Param("permissionId") long permissionId);
    /**
     * 权限数量处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_permission</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 符合 SQL 条件的记录数量
     */
    int permissionCount(@Param("id") long id);
    /**
     * 角色权限数量处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_role_permission</code>。
     *
     * @param roleId 角色标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("roleId")</code>
     * @param permissionId 权限标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("permissionId")</code>
     * @return 符合 SQL 条件的记录数量
     */
    int rolePermissionCount(@Param("roleId") long roleId, @Param("permissionId") long permissionId);

    /**
     * 账号分页数量处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_account</code>。
     *
     * @param keyword 关键词；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("keyword")</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("status")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long accountPageCount(@Param("keyword") String keyword, @Param("status") String status);
    /**
     * 账号分页处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_account</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param keyword 关键词；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("keyword")</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("status")</code>
     * @param offset 查询跳过的记录数；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("offset")</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("limit")</code>
     * @return 本次处理得到的条目集合
     */
    List<AccountEntity> accountPage(@Param("keyword") String keyword, @Param("status") String status,
                                          @Param("offset") int offset, @Param("limit") int limit);

    /**
     * 写入邀请。
     *
     * 持久化操作：新增，涉及 <code>sr_admin_invitation</code>。
     *
     * @param invitation 邀请；类型为 <code>AdminInvitationEntity</code>
     */
    void insertInvitation(AdminInvitationEntity invitation);
    /**
     * 按标识读取邀请。
     *
     * 持久化操作：读取，涉及 <code>sr_admin_invitation</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>AdminInvitationEntity</code> 的处理结果
     */
    AdminInvitationEntity invitationById(@Param("id") long id);
    /**
     * 按标识加行锁读取邀请。
     *
     * 持久化操作：读取，涉及 <code>sr_admin_invitation</code>。
     * 此查询使用行锁；调用方应在事务中完成后续状态检查和写入。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>AdminInvitationEntity</code> 的处理结果
     */
    AdminInvitationEntity invitationByIdForUpdate(@Param("id") long id);
    /**
     * 按摘要读取邀请。
     *
     * 持久化操作：读取，涉及 <code>sr_admin_invitation</code>。
     *
     * @param hash 摘要；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("hash")</code>
     * @return 类型为 <code>AdminInvitationEntity</code> 的处理结果
     */
    AdminInvitationEntity invitationByHash(@Param("hash") String hash);
    /**
     * 邀请分页处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_admin_invitation</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param offset 查询跳过的记录数；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("offset")</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("limit")</code>
     * @return 本次处理得到的条目集合
     */
    List<AdminInvitationEntity> invitationPage(@Param("offset") int offset, @Param("limit") int limit);
    /**
     * 邀请分页数量处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_admin_invitation</code>。
     * @return 符合 SQL 条件的记录数量
     */
    long invitationPageCount();
    /**
     * 待处理Invitations面向账号处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_admin_invitation</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @return 本次处理得到的条目集合
     */
    List<AdminInvitationEntity> pendingInvitationsForAccount(@Param("accountId") long accountId);
    /**
     * 保存邀请Delivery。
     *
     * 持久化操作：更新，涉及 <code>sr_admin_invitation</code>。
     *
     * @param invitation 邀请；类型为 <code>AdminInvitationEntity</code>
     */
    void saveInvitationDelivery(AdminInvitationEntity invitation);
    /**
     * 撤销邀请。
     *
     * 持久化操作：更新，涉及 <code>sr_admin_invitation</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     */
    void revokeInvitation(@Param("id") long id);
    /**
     * 按摘要面向更新读取邀请。
     *
     * 持久化操作：读取，涉及 <code>sr_admin_invitation</code>。
     * 此查询使用行锁；调用方应在事务中完成后续状态检查和写入。
     *
     * @param hash 摘要；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("hash")</code>
     * @return 类型为 <code>AdminInvitationEntity</code> 的处理结果
     */
    AdminInvitationEntity invitationByHashForUpdate(@Param("hash") String hash);
    /**
     * 待处理邀请数量处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_admin_invitation</code>。
     *
     * @param targetId 被处理或审核的目标对象标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("targetId")</code>
     * @return 符合 SQL 条件的记录数量
     */
    int pendingInvitationCount(@Param("targetId") long targetId);
    /**
     * 接受邀请。
     *
     * 持久化操作：更新，涉及 <code>sr_admin_invitation</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @return SQL 写入影响的记录行数
     */
    int acceptInvitation(@Param("id") long id, @Param("accountId") long accountId);
    /**
     * 撤销待处理Invitations。
     *
     * 持久化操作：更新，涉及 <code>sr_admin_invitation</code>。
     *
     * @param targetId 被处理或审核的目标对象标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("targetId")</code>
     * @return SQL 写入影响的记录行数
     */
    int revokePendingInvitations(@Param("targetId") long targetId);

    /**
     * 写入Reset。
     *
     * 持久化操作：新增，涉及 <code>sr_password_reset</code>。
     *
     * @param reset reset；类型为 <code>PasswordResetEntity</code>
     */
    void insertReset(PasswordResetEntity reset);
    /**
     * 重置依据摘要面向更新。
     *
     * 持久化操作：读取，涉及 <code>sr_password_reset</code>。
     * 此查询使用行锁；调用方应在事务中完成后续状态检查和写入。
     *
     * @param hash 摘要；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("hash")</code>
     * @return 类型为 <code>PasswordResetEntity</code> 的处理结果
     */
    PasswordResetEntity resetByHashForUpdate(@Param("hash") String hash);

    /** Resolves immutable ownership before acquiring account then token locks in a consistent order. */
    PasswordResetEntity resetByHash(@Param("hash") String hash);
    /**
     * useReset处理入口。
     *
     * 持久化操作：更新，涉及 <code>sr_password_reset</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return SQL 写入影响的记录行数
     */
    int useReset(@Param("id") long id);
    /**
     * expire待处理Resets处理入口。
     *
     * 持久化操作：更新，涉及 <code>sr_password_reset</code>。
     *
     * @param accountId 账号标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("accountId")</code>
     * @return SQL 写入影响的记录行数
     */
    int expirePendingResets(@Param("accountId") long accountId);
    /**
     * 按IP 地址读取recentResetRequests。
     *
     * 持久化操作：读取，涉及 <code>sr_account_audit</code>。
     *
     * @param ip IP 地址；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("ip")</code>
     * @param since since；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("since")</code>
     * @return 符合 SQL 条件的记录数量
     */
    int recentResetRequestsByIp(@Param("ip") String ip, @Param("since") LocalDateTime since);
    /**
     * recent审计数量处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_account_audit</code>。
     *
     * @param ip IP 地址；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("ip")</code>
     * @param action 动作；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("action")</code>
     * @param since since；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("since")</code>
     * @return 符合 SQL 条件的记录数量
     */
    int recentAuditCount(@Param("ip") String ip, @Param("action") String action,
                         @Param("since") LocalDateTime since);

    /**
     * 写入审计。
     *
     * 持久化操作：新增，涉及 <code>sr_account_audit</code>。
     *
     * @param audit 审计；类型为 <code>AccountAuditEntity</code>
     */
    void insertAudit(AccountAuditEntity audit);
    /**
     * 审计分页数量处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_account_audit</code>。
     *
     * @param actor 当前操作对应的身份对象；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("actor")</code>
     * @param target 目标；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("target")</code>
     * @param action 动作；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("action")</code>
     * @param result 结果；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("result")</code>
     * @param start 开始；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("start")</code>
     * @param end 结束；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("end")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long auditPageCount(@Param("actor") Long actor, @Param("target") Long target,
                        @Param("action") String action, @Param("result") String result,
                        @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    /**
     * 审计分页处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_account_audit</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param actor 当前操作对应的身份对象；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("actor")</code>
     * @param target 目标；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("target")</code>
     * @param action 动作；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("action")</code>
     * @param result 结果；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("result")</code>
     * @param start 开始；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("start")</code>
     * @param end 结束；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("end")</code>
     * @param offset 查询跳过的记录数；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("offset")</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("limit")</code>
     * @return 本次处理得到的条目集合
     */
    List<AccountAuditEntity> auditPage(@Param("actor") Long actor, @Param("target") Long target,
                                      @Param("action") String action, @Param("result") String result,
                                      @Param("start") LocalDateTime start, @Param("end") LocalDateTime end,
                                      @Param("offset") int offset, @Param("limit") int limit);
}
