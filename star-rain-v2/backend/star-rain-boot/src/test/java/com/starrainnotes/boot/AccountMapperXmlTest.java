package com.starrainnotes.boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.starrainnotes.account.entity.AccountAuditEntity;
import com.starrainnotes.account.entity.AccountEntity;
import com.starrainnotes.account.entity.AdminInvitationEntity;
import com.starrainnotes.account.entity.EmailVerificationEntity;
import com.starrainnotes.account.entity.PasswordResetEntity;
import com.starrainnotes.account.entity.PermissionEntity;
import com.starrainnotes.account.entity.RoleEntity;
import com.starrainnotes.account.mapper.AccountMapper;
import com.starrainnotes.account.mapper.EmailVerificationMapper;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/*
 * account 模块 2 个 Mapper（AccountMapper、EmailVerificationMapper）的真实数据库读写验证。
 *
 * 这两个 Mapper 的语句在单元测试里全被 Mockito 挡掉，XML 从没被 MyBatis 解析过。
 * 这里专门盯住「注解 SQL 搬进 XML」最容易出错的地方：
 *   1. 自增主键回填（insertAccount / insertInvitation / insertReset / insertAudit）；
 *   2. 带状态守卫的 UPDATE 只能生效一次（verifyEmail、acceptInvitation、useReset、consume）；
 *   3. `INSERT IGNORE` 依赖的唯一键幂等（insertAccountRole、insertRolePermission）；
 *   4. `<where>` + `<if>` 拼出来的 accountFilters / auditFilters 在「条件为 null」时是否退化正确；
 *   5. 只 SELECT 部分列的两个语句（invitationByHashForUpdate、resetByHashForUpdate）读回的值。
 *
 * 全程在一个不 commit 的 SqlSession 里跑，@AfterEach 回滚。本类会临时改动 sr_role_permission
 * 里 ADMIN 角色的挂载关系（deleteRolePermissions → 重新 insert），但都在同一个未提交事务里，
 * 回滚后开发库恢复原样；最后一个用例用新连接复核。
 *
 * 时区：生产 application.yml 给连接配了 connection-init-sql: SET time_zone = '+00:00'，
 * 而 AccountAuthServiceImpl / AdminAccountServiceImpl / EmailVerificationServiceImpl 一律用
 * LocalDateTime.now(ZoneOffset.UTC) 计算过期时间；XML 里也混着 CURRENT_TIMESTAMP() 与
 * UTC_TIMESTAMP()。验证骨架用的是裸 UnpooledDataSource，不执行这条 init SQL，本机 MySQL 的
 * session 时区又是 SYSTEM(UTC+8)，两套时钟会差 8 小时，过期/未过期的边界断言就会失真。
 * 所以这里在 @BeforeEach 显式把本会话的 time_zone 对齐成生产值，并用 UTC 时钟造数据。
 */
class AccountMapperXmlTest extends MapperXmlIntegrationSupport {

    private static final AtomicLong PROBE = new AtomicLong(System.nanoTime() % 1_000_000L);

    private SqlSession session;
    private AccountMapper accounts;
    private EmailVerificationMapper verifications;

    @BeforeEach
    void open() throws SQLException {
        session = openSession();
        try (Statement statement = session.getConnection().createStatement()) {
            statement.execute("SET time_zone = '+00:00'");
        }
        accounts = session.getMapper(AccountMapper.class);
        verifications = session.getMapper(EmailVerificationMapper.class);
    }

    @AfterEach
    void rollback() {
        if (session != null) {
            session.rollback();
            session.close();
        }
    }

    @Test
    void everyAccountMapperIsReachableThroughTheSession() {
        assertNotNull(accounts);
        assertNotNull(verifications);
    }

    @Test
    void insertedAccountIsReadableByEveryLookupAndBackfillsItsId() {
        AccountEntity probe = newAccount("ACTIVE");
        accounts.insertAccount(probe);
        assertNotNull(probe.getId(), "insertAccount 没有把自增主键回填到实体上");

        AccountEntity loaded = accounts.accountById(probe.getId());
        assertNotNull(loaded, "accountById 读不到刚插入的行");
        assertEquals(probe.getUsername(), loaded.getUsername());
        assertEquals(probe.getEmail(), loaded.getEmail());
        assertEquals(probe.getDisplayName(), loaded.getDisplayName());
        assertEquals("ACTIVE", loaded.getStatus());
        assertEquals(1, loaded.getAuthVersion().intValue(), "auth_version 应当取列默认值 1");
        assertNull(loaded.getEmailVerifiedAt());
        assertNull(loaded.getLastLoginAt());
        assertNotNull(loaded.getCreatedAt(), "created_at 由列默认值填充，应能读回");

        assertEquals(probe.getId(), accounts.accountByIdentifier(probe.getUsername()).getId(),
                "accountByIdentifier 必须能用 username 命中");
        assertEquals(probe.getId(), accounts.accountByIdentifier(probe.getEmail()).getId(),
                "accountByIdentifier 必须能用 email 命中");
        assertEquals(probe.getId(), accounts.accountByEmail(probe.getEmail()).getId());
        assertEquals(1, accounts.usernameCount(probe.getUsername()));
        assertEquals(1, accounts.emailCount(probe.getEmail()));

        assertEquals(probe.getId(), accounts.accountByIdForUpdate(probe.getId()).getId(),
                "FOR UPDATE 读不到刚插入的行");

        assertNull(accounts.accountById(-1L), "不存在的 id 应返回 null");
        assertNull(accounts.accountByEmail(unique("mapperxmlmissing") + "@example.test"));
        assertEquals(0, accounts.usernameCount(unique("mapperxmlmissing")));
        assertEquals(0, accounts.emailCount(unique("mapperxmlmissing") + "@example.test"));
    }

    @Test
    void emailVerificationStatusChangesAndVersionBumpsLandOnTheRightColumns() {
        AccountEntity probe = newAccount("ACTIVE");
        accounts.insertAccount(probe);

        assertEquals(1, accounts.verifyEmail(probe.getId()));
        assertEquals(0, accounts.verifyEmail(probe.getId()), "已经验证过的邮箱不能被二次验证");
        assertNotNull(accounts.accountById(probe.getId()).getEmailVerifiedAt());

        assertEquals(1, accounts.updateDisplayName(probe.getId(), "XML 验证改名"));
        AccountEntity renamed = accounts.accountById(probe.getId());
        assertEquals("XML 验证改名", renamed.getDisplayName());
        assertEquals("ACTIVE", renamed.getStatus(), "改显示名不应动 status");
        assertEquals(1, renamed.getAuthVersion().intValue(), "改显示名不应动 auth_version");
        assertNotNull(renamed.getEmailVerifiedAt(), "改显示名不应清掉验证时间");

        assertEquals(1, accounts.updateLastLogin(probe.getId()));
        assertNotNull(accounts.accountById(probe.getId()).getLastLoginAt());

        assertEquals(1, accounts.incrementAuthVersion(probe.getId()));
        assertEquals(2, accounts.accountById(probe.getId()).getAuthVersion().intValue());

        assertEquals(1, accounts.updateStatus(probe.getId(), "DISABLED"));
        AccountEntity disabled = accounts.accountById(probe.getId());
        assertEquals("DISABLED", disabled.getStatus());
        assertEquals(3, disabled.getAuthVersion().intValue(),
                "改状态必须让旧令牌失效（auth_version 递增）");

        assertEquals(0, accounts.updateStatus(-1L, "DISABLED"));
        assertEquals(0, accounts.incrementAuthVersion(-1L));
        assertEquals(0, accounts.updateDisplayName(-1L, "不存在"));
        assertEquals(0, accounts.verifyEmail(-1L));
    }

    @Test
    void credentialsRoundTripAndPasswordChangeRewritesTheHash() {
        AccountEntity probe = newAccount("ACTIVE");
        accounts.insertAccount(probe);
        assertNull(accounts.passwordHash(probe.getId()), "新建账户还没有凭据");

        accounts.insertCredential(probe.getId(), "mapper-xml-test-hash-one");
        assertEquals("mapper-xml-test-hash-one", accounts.passwordHash(probe.getId()));

        assertEquals(1, accounts.updatePassword(probe.getId(), "mapper-xml-test-hash-two"));
        assertEquals("mapper-xml-test-hash-two", accounts.passwordHash(probe.getId()));
        assertEquals(0, accounts.updatePassword(-1L, "mapper-xml-test-hash-three"));
        assertNull(accounts.passwordHash(-1L));
    }

    @Test
    void roleAndPermissionWiringReadsBackAndGrantsAreIdempotent() {
        RoleEntity superAdmin = accounts.roleByCode("SUPER_ADMIN");
        assertNotNull(superAdmin, "sr_role 里没有 SUPER_ADMIN，检查 V2_001 迁移");
        assertEquals(superAdmin.getId(), accounts.roleById(superAdmin.getId()).getId());
        assertTrue(accounts.allRoles().stream().anyMatch(role -> "USER".equals(role.getCode())),
                "allRoles 至少要带回 USER 角色");

        PermissionEntity permission = accounts.allPermissions().get(0);
        assertNotNull(permission.getCode());
        assertNotNull(permission.getResource());
        assertNotNull(permission.getAction());
        assertEquals(1, accounts.permissionCount(permission.getId()));
        assertEquals(0, accounts.permissionCount(-1L), "不存在的权限不应算作 ENABLED");

        AccountEntity probe = newAccount("ACTIVE");
        accounts.insertAccount(probe);
        int assignments = accounts.superAdminAssignmentCount();
        int active = accounts.activeSuperAdminCount();

        accounts.insertAccountRole(probe.getId(), superAdmin.getId(), null);
        assertEquals(List.of(superAdmin.getId()), accounts.roleIds(probe.getId()));
        assertTrue(accounts.roleCodes(probe.getId()).contains("SUPER_ADMIN"));
        assertFalse(accounts.permissionCodes(probe.getId()).isEmpty(),
                "SUPER_ADMIN 的权限码应当能通过三表 JOIN 查出来");
        assertEquals(assignments + 1, accounts.superAdminAssignmentCount());
        assertEquals(active + 1, accounts.activeSuperAdminCount());

        accounts.insertAccountRole(probe.getId(), superAdmin.getId(), null);
        assertEquals(1, accounts.roleIds(probe.getId()).size(),
                "uk_sr_account_role 唯一键 + INSERT IGNORE 应当让重复授予不再新增行");
        assertEquals(assignments + 1, accounts.superAdminAssignmentCount());

        assertEquals(1, accounts.deleteAccountRole(probe.getId(), superAdmin.getId()));
        assertEquals(0, accounts.deleteAccountRole(probe.getId(), superAdmin.getId()));
        assertTrue(accounts.roleIds(probe.getId()).isEmpty());
        assertTrue(accounts.roleCodes(probe.getId()).isEmpty());
        assertTrue(accounts.permissionCodes(probe.getId()).isEmpty());
        assertEquals(assignments, accounts.superAdminAssignmentCount());

        accounts.insertAccountRole(probe.getId(), superAdmin.getId(), null);
        assertEquals(1, accounts.deleteAccountRoles(probe.getId()));
        assertEquals(0, accounts.deleteAccountRoles(probe.getId()));

        assertNotNull(accounts.lockSuperAdminRole(), "FOR UPDATE 锁住 SUPER_ADMIN 角色应当有返回");
        assertEquals(superAdmin.getId(), accounts.lockSuperAdminRole());
        assertTrue(accounts.activeSuperAdminCount() >= 0);
    }

    @Test
    void rolePermissionMappingsAreRewrittenInsideTheUncommittedTransaction() {
        RoleEntity admin = accounts.roleByCode("ADMIN");
        assertNotNull(admin, "sr_role 里没有 ADMIN");
        Long permissionId = accounts.allPermissions().get(0).getId();

        assertEquals(0, accounts.rolePermissionCount(admin.getId(), -1L));

        int removed = accounts.deleteRolePermissions(admin.getId());
        assertTrue(removed >= 1, "ADMIN 角色原本应当挂着权限，检查 V2_001 种子数据");
        assertTrue(accounts.permissionIdsForRole(admin.getId()).isEmpty());

        accounts.insertRolePermission(admin.getId(), permissionId);
        assertEquals(1, accounts.rolePermissionCount(admin.getId(), permissionId));
        accounts.insertRolePermission(admin.getId(), permissionId);
        assertEquals(1, accounts.rolePermissionCount(admin.getId(), permissionId),
                "uk_sr_role_permission 唯一键 + INSERT IGNORE 应当保持幂等");
        assertEquals(1, accounts.permissionIdsForRole(admin.getId()).size());
        assertEquals(permissionId, accounts.permissionIdsForRole(admin.getId()).get(0));

        assertEquals(1, accounts.deleteRolePermissions(admin.getId()));
        assertTrue(accounts.permissionIdsForRole(admin.getId()).isEmpty());
        assertEquals(0, accounts.rolePermissionCount(admin.getId(), permissionId));
    }

    @Test
    void incrementVersionsForRoleBumpsEveryMemberAccount() {
        AccountEntity probe = newAccount("ACTIVE");
        accounts.insertAccount(probe);
        int before = accounts.accountById(probe.getId()).getAuthVersion();
        RoleEntity admin = accounts.roleByCode("ADMIN");
        accounts.insertAccountRole(probe.getId(), admin.getId(), null);

        int affected = accounts.incrementVersionsForRole(admin.getId());
        assertTrue(affected >= 1, "至少要影响刚授予 ADMIN 的探针账户");
        assertEquals(before + 1, accounts.accountById(probe.getId()).getAuthVersion().intValue(),
                "角色变更必须让该角色的所有成员 auth_version 递增");
    }

    @Test
    void accountPageFiltersByKeywordAndStatusAndPagesNewestFirst() {
        String keyword = unique("mapperxmlpage");
        AccountEntity active = newAccount("ACTIVE");
        active.setUsername(keyword + "-a");
        accounts.insertAccount(active);
        AccountEntity disabled = newAccount("DISABLED");
        disabled.setUsername(keyword + "-d");
        accounts.insertAccount(disabled);

        assertEquals(2, accounts.accountPageCount(keyword, null), "keyword 过滤（username LIKE）没生效");
        assertEquals(1, accounts.accountPageCount(keyword, "ACTIVE"), "status 过滤没生效");
        assertEquals(1, accounts.accountPageCount(keyword, "DISABLED"));
        assertEquals(0, accounts.accountPageCount(keyword, "SUSPENDED"));
        assertEquals(0, accounts.accountPageCount(unique("mapperxmlpage-missing"), null));
        assertTrue(accounts.accountPageCount(null, null) >= 2,
                "条件全为 null 时必须退化成统计全表（否则会把开发库里的真实账户漏掉）");

        List<AccountEntity> page = accounts.accountPage(keyword, null, 0, 10);
        assertEquals(2, page.size());
        assertEquals(disabled.getId(), page.get(0).getId(), "分页必须按 id DESC");
        assertEquals(active.getId(), page.get(1).getId());
        assertEquals(active.getEmail(), page.get(1).getEmail(), "列清单漏了 email");

        assertEquals(1, accounts.accountPage(keyword, "ACTIVE", 0, 10).size());
        assertEquals(1, accounts.accountPage(keyword, null, 0, 1).size(), "LIMIT 没生效");
        assertEquals(active.getId(), accounts.accountPage(keyword, null, 1, 1).get(0).getId(),
                "OFFSET 没生效");
    }

    @Test
    void invitationLifecycleGuardsPendingStatusAndExpiry() {
        AccountEntity inviter = newAccount("ACTIVE");
        accounts.insertAccount(inviter);
        AccountEntity target = newAccount("ACTIVE");
        accounts.insertAccount(target);
        LocalDateTime dbNow = now();

        String hash = token64(unique("mapperxmlinvite"));
        AdminInvitationEntity invitation = newInvitation(target.getId(), target.getEmail(), hash, inviter.getId(),
                dbNow.plusDays(1));
        accounts.insertInvitation(invitation);
        assertNotNull(invitation.getId(), "insertInvitation 没有回填自增主键");

        AdminInvitationEntity loaded = accounts.invitationById(invitation.getId());
        assertNotNull(loaded);
        assertEquals(hash, loaded.getTokenHash());
        assertEquals(target.getId(), loaded.getTargetAccountId());
        assertEquals(inviter.getId(), loaded.getInvitedBy());
        assertEquals("ADMIN", loaded.getTargetRoleCode());
        assertEquals("PENDING", loaded.getStatus());
        assertEquals("UNKNOWN", loaded.getMailSubmissionStatus(), "mail_submission_status 应取列默认值");
        assertNull(loaded.getLastSentAt());
        assertNull(loaded.getMailMessageId());
        assertNull(loaded.getAcceptedAt());
        assertNull(loaded.getRevokedAt());
        assertNotNull(loaded.getCreatedAt(), "created_at 由列默认值填充，应能读回");
        assertEquals(target.getEmail(), loaded.getEmailSnapshot());

        assertEquals(invitation.getId(), accounts.invitationByHash(hash).getId());
        assertNull(accounts.invitationByHash(token64(unique("mapperxmlmissinginvite"))));
        assertNotNull(accounts.invitationByIdForUpdate(invitation.getId()));
        AdminInvitationEntity forUpdate = accounts.invitationByHashForUpdate(hash);
        assertNotNull(forUpdate);
        assertEquals(invitation.getId(), forUpdate.getId());
        assertEquals("PENDING", forUpdate.getStatus(), "只 SELECT 部分列的语句也要映射 status");

        assertTrue(accounts.invitationPageCount() >= 1);
        assertEquals(invitation.getId(), accounts.invitationPage(0, 1).get(0).getId(),
                "邀请分页应按 id DESC，刚插入的排第一");
        assertTrue(accounts.invitationPage(0, 50).stream()
                .anyMatch(item -> item.getId().equals(invitation.getId())));

        assertEquals(1, accounts.pendingInvitationsForAccount(target.getId()).size(),
                "PENDING 且未过期的邀请应当被查到");
        assertEquals(1, accounts.pendingInvitationCount(target.getId()));

        // 重新投递：换 token、重置状态、记录邮件回执
        String redelivered = token64(unique("mapperxmlinvite-resend"));
        invitation.setTokenHash(redelivered);
        invitation.setExpiresAt(dbNow.plusDays(2));
        invitation.setLastSentAt(dbNow);
        invitation.setMailMessageId("mapper-xml-test-message");
        accounts.saveInvitationDelivery(invitation);
        AdminInvitationEntity afterDelivery = accounts.invitationById(invitation.getId());
        assertEquals(redelivered, afterDelivery.getTokenHash());
        assertEquals("PENDING", afterDelivery.getStatus(), "saveInvitationDelivery 必须把状态重置成 PENDING");
        assertEquals("SUBMITTED", afterDelivery.getMailSubmissionStatus());
        assertNotNull(afterDelivery.getLastSentAt());
        assertEquals("mapper-xml-test-message", afterDelivery.getMailMessageId());
        assertNull(accounts.invitationByHash(hash), "旧 token 应当查不到了");

        assertEquals(1, accounts.acceptInvitation(invitation.getId(), target.getId()));
        assertEquals(0, accounts.acceptInvitation(invitation.getId(), target.getId()),
                "已接受的邀请不能被二次接受");
        AdminInvitationEntity accepted = accounts.invitationById(invitation.getId());
        assertEquals("ACCEPTED", accepted.getStatus());
        assertNotNull(accepted.getAcceptedAt());
        /*
         * 这里曾经是本测试发现的一个真实缺陷：invitationColumns 的列清单里没有 accepted_by，
         * 而 acceptInvitation 会写这一列，所以实体的 acceptedBy 恒为 null —— 同一份 XML 自己写、自己读不出来。
         * 已把 accepted_by 补进列清单，因此现在直接断言实体能读到它；
         * 下面再走一条绕过 Mapper 的直读库路径作为独立交叉验证（写路径本身确实生效）。
         */
        assertEquals(target.getId(), accepted.getAcceptedBy(),
                "invitationColumns 必须包含 accepted_by，否则写进去的接手人读不出来");
        assertEquals(target.getId().longValue(), acceptedById(invitation.getId()),
                "acceptInvitation 没有把接手人写进 accepted_by");
        assertEquals(0, accounts.pendingInvitationCount(target.getId()));
        assertTrue(accounts.pendingInvitationsForAccount(target.getId()).isEmpty());

        // 已过期的邀请既不能进待处理列表，也不能被接受
        AdminInvitationEntity expired = newInvitation(target.getId(), target.getEmail(), token64(unique("mapperxmlinvite-expired")),
                inviter.getId(), dbNow.minusHours(1));
        accounts.insertInvitation(expired);
        assertTrue(accounts.pendingInvitationsForAccount(target.getId()).isEmpty(),
                "expires_at 已过的邀请不应出现在待处理列表");
        assertEquals(0, accounts.pendingInvitationCount(target.getId()));
        assertEquals(0, accounts.acceptInvitation(expired.getId(), target.getId()),
                "expires_at 已过的邀请不能被接受");
        assertEquals("PENDING", accounts.invitationById(expired.getId()).getStatus(),
                "被拒绝的接受不应改动状态");

        // 撤销：单个 / 批量，且都只对 PENDING 生效
        AdminInvitationEntity revocable = newInvitation(target.getId(), target.getEmail(), token64(unique("mapperxmlinvite-revoke")),
                inviter.getId(), dbNow.plusDays(1));
        accounts.insertInvitation(revocable);
        assertEquals(1, accounts.pendingInvitationCount(target.getId()));
        // 批量撤销刻意不看过期时间：PENDING 的积压（含已过期的 expired 那条）一次清干净
        assertEquals(2, accounts.revokePendingInvitations(target.getId()),
                "批量撤销应当把 PENDING 的积压（含已过期但状态仍是 PENDING 的）一起收尾");
        assertEquals(0, accounts.revokePendingInvitations(target.getId()), "已经不是 PENDING，不该再撤销一次");
        AdminInvitationEntity revoked = accounts.invitationById(revocable.getId());
        assertEquals("REVOKED", revoked.getStatus());
        assertNotNull(revoked.getRevokedAt());

        AdminInvitationEntity single = newInvitation(target.getId(), target.getEmail(), token64(unique("mapperxmlinvite-single")),
                inviter.getId(), dbNow.plusDays(1));
        accounts.insertInvitation(single);
        accounts.revokeInvitation(single.getId());
        assertEquals("REVOKED", accounts.invitationById(single.getId()).getStatus());
        assertEquals(0, accounts.revokePendingInvitations(target.getId()));
    }

    @Test
    void passwordResetTokensAreConsumedOnceAndPendingOnesExpirePerAccount() {
        AccountEntity probe = newAccount("ACTIVE");
        accounts.insertAccount(probe);
        LocalDateTime dbNow = now();

        PasswordResetEntity reset = newReset(probe.getId(), token64(unique("mapperxmlreset")),
                dbNow.plusHours(1));
        accounts.insertReset(reset);
        assertNotNull(reset.getId(), "insertReset 没有回填自增主键");

        PasswordResetEntity loaded = accounts.resetByHashForUpdate(reset.getTokenHash());
        assertNotNull(loaded, "resetByHashForUpdate 读不到刚插入的令牌");
        assertEquals(probe.getId(), loaded.getAccountId());
        assertEquals("PENDING", loaded.getStatus());
        assertNotNull(loaded.getExpiresAt());
        assertNull(loaded.getUsedAt());
        assertNull(accounts.resetByHashForUpdate(token64(unique("mapperxmlmissingreset"))));

        assertEquals(1, accounts.useReset(reset.getId()));
        assertEquals(0, accounts.useReset(reset.getId()), "同一个重置令牌不能被使用两次");
        // resetByHashForUpdate 只 SELECT 了 id/account_id/token_hash/status/expires_at，
        // 所以这里只能看 status；used_at 不在列清单里，按设计读回来必然是 null
        assertEquals("USED", accounts.resetByHashForUpdate(reset.getTokenHash()).getStatus());

        PasswordResetEntity expired = newReset(probe.getId(), token64(unique("mapperxmlreset-expired")),
                dbNow.minusHours(1));
        accounts.insertReset(expired);
        assertEquals(0, accounts.useReset(expired.getId()), "已过期的令牌不能被使用");
        assertEquals(0, accounts.useReset(-1L), "不存在的令牌不能被使用");

        // expirePendingResets 只处理 status = PENDING：用另一个账户把范围隔离出来
        AccountEntity expiring = newAccount("ACTIVE");
        accounts.insertAccount(expiring);
        PasswordResetEntity pending = newReset(expiring.getId(), token64(unique("mapperxmlreset-pending")),
                dbNow.plusHours(1));
        accounts.insertReset(pending);
        assertEquals(1, accounts.expirePendingResets(expiring.getId()));
        assertEquals(0, accounts.expirePendingResets(expiring.getId()), "已经没有 PENDING 了");
        assertEquals("EXPIRED", accounts.resetByHashForUpdate(pending.getTokenHash()).getStatus());
        assertEquals("PENDING", accounts.resetByHashForUpdate(expired.getTokenHash()).getStatus(),
                "别人的账户不该被顺手过期");
    }

    @Test
    void auditRowsRoundTripAndEveryOptionalFilterNarrowsThePage() {
        AccountEntity actor = newAccount("ACTIVE");
        accounts.insertAccount(actor);
        AccountEntity target = newAccount("ACTIVE");
        accounts.insertAccount(target);
        String ip = "203.0.113." + (PROBE.incrementAndGet() % 200 + 1);
        LocalDateTime since = now().minusHours(1).withNano(0);

        AccountAuditEntity first = newAudit(actor.getId(), null, "PASSWORD_RESET_REQUESTED", "SUCCESS", ip);
        accounts.insertAudit(first);
        assertNotNull(first.getId(), "insertAudit 没有回填自增主键");
        AccountAuditEntity second = newAudit(actor.getId(), target.getId(), "LOGIN_FAILED", "FAILURE", ip);
        accounts.insertAudit(second);
        assertNotNull(second.getId());

        AccountAuditEntity loaded = accounts.auditPage(actor.getId(), null, null, null, null, null, 0, 10).get(0);
        assertEquals(second.getId(), loaded.getId(), "审计分页必须按 id DESC");
        assertEquals("LOGIN_FAILED", loaded.getActionCode());
        assertEquals("FAILURE", loaded.getResult());
        assertEquals(ip, loaded.getIpAddress());
        assertEquals("mapper-xml-test-agent", loaded.getUserAgent());
        assertEquals(target.getId(), loaded.getTargetAccountId());
        assertNotNull(loaded.getCreatedAt(), "created_at 由列默认值填充，应能读回");

        assertEquals(2, accounts.auditPageCount(actor.getId(), null, null, null, null, null));
        assertEquals(1, accounts.auditPageCount(actor.getId(), target.getId(), null, null, null, null),
                "target 过滤没生效");
        assertEquals(0, accounts.auditPageCount(actor.getId(), -1L, null, null, null, null));
        assertEquals(1, accounts.auditPageCount(actor.getId(), null, "PASSWORD_RESET_REQUESTED", null, null, null),
                "action 过滤没生效");
        assertEquals(1, accounts.auditPageCount(actor.getId(), null, null, "FAILURE", null, null),
                "result 过滤没生效");
        assertEquals(0, accounts.auditPageCount(actor.getId(), null, null, "SUCCESS_UNKNOWN", null, null));
        assertEquals(2, accounts.auditPageCount(actor.getId(), null, null, null, since, null),
                "start 过滤（created_at >= since）没生效");
        assertEquals(0, accounts.auditPageCount(actor.getId(), null, null, null, null, since),
                "end 过滤（created_at <= since）没生效");
        assertEquals(0, accounts.auditPageCount(-1L, null, null, null, null, null));

        assertEquals(second.getId(), accounts.auditPage(actor.getId(), null, null, null, null, null, 0, 1)
                .get(0).getId(), "LIMIT 没生效");
        assertEquals(first.getId(), accounts.auditPage(actor.getId(), null, null, null, null, null, 1, 1)
                .get(0).getId(), "OFFSET 没生效");
        List<AccountAuditEntity> onlyFailures = accounts.auditPage(actor.getId(), null, null, "FAILURE", null, null, 0, 10);
        assertEquals(1, onlyFailures.size());
        assertEquals(second.getId(), onlyFailures.get(0).getId());

        assertEquals(1, accounts.recentResetRequestsByIp(ip, since), "按 IP 统计重置申请没生效");
        assertEquals(0, accounts.recentResetRequestsByIp(ip, now().plusHours(1)),
                "since 之后的窗口不应命中");
        assertEquals(1, accounts.recentAuditCount(ip, "LOGIN_FAILED", since));
        assertEquals(0, accounts.recentAuditCount(ip, "LOGIN_SUCCESS", since));
        assertEquals(0, accounts.recentAuditCount(ip, "LOGIN_FAILED", now().plusHours(1)),
                "since 之后的窗口不应命中任何审计行");
    }

    @Test
    void emailVerificationIssuesConsumesAndLocksOutCodes() {
        String email = unique("mapperxmlverify") + "@example.test";
        verifications.ensureEmail(email);
        EmailVerificationEntity created = verifications.findForUpdate(email);
        assertNotNull(created, "ensureEmail 之后必须能查到行");
        assertEquals(email, created.getEmail());
        assertNull(created.getCodeHash());
        assertEquals(0, created.getSendCount(), "send_count 应当取列默认值 0");
        assertEquals(0, created.getFailedAttempts(), "failed_attempts 应当取列默认值 0");
        assertNull(created.getConsumedAt());

        // 重复 ensureEmail 走 ON DUPLICATE KEY UPDATE，不应抛异常
        verifications.ensureEmail(email);
        assertNotNull(verifications.findForUpdate(email));

        LocalDateTime dbNow = now();
        String goodHash = token64(unique("mapperxmlcode"));
        EmailVerificationEntity code = new EmailVerificationEntity();
        code.setEmail(email);
        code.setCodeHash(goodHash);
        code.setExpiresAt(dbNow.plusHours(1));
        code.setSentAt(dbNow);
        code.setWindowStartedAt(dbNow);
        code.setSendCount(1);
        verifications.saveCode(code);

        EmailVerificationEntity saved = verifications.findForUpdate(email);
        assertEquals(goodHash, saved.getCodeHash(), "saveCode 没有写进验证码哈希");
        assertEquals(1, saved.getSendCount());
        assertEquals(0, saved.getFailedAttempts(), "saveCode 必须把失败次数清零");
        assertNull(saved.getConsumedAt(), "saveCode 必须把 consumed_at 清成 NULL，允许重新消费新码");
        assertNotNull(saved.getSentAt());
        assertNotNull(saved.getWindowStartedAt());
        assertNotNull(saved.getExpiresAt());

        verifications.incrementFailures(email);
        assertEquals(1, verifications.findForUpdate(email).getFailedAttempts(),
                "incrementFailures 必须在原值上累加");

        assertEquals(0, verifications.consume(email, token64(unique("mapperxmlwrongcode"))),
                "验证码不匹配时不能消费");
        assertEquals(1, verifications.consume(email, goodHash));
        assertEquals(0, verifications.consume(email, goodHash), "同一个验证码不能被消费两次");
        assertNotNull(verifications.findForUpdate(email).getConsumedAt());
    }

    @Test
    void expiredCodeAndTooManyFailuresAreBothRejected() {
        LocalDateTime dbNow = now();

        String expiredEmail = unique("mapperxmlverify-expired") + "@example.test";
        verifications.ensureEmail(expiredEmail);
        String expiredHash = token64(unique("mapperxmlcode-expired"));
        EmailVerificationEntity expired = new EmailVerificationEntity();
        expired.setEmail(expiredEmail);
        expired.setCodeHash(expiredHash);
        expired.setExpiresAt(dbNow.minusHours(1));
        expired.setSentAt(dbNow.minusHours(2));
        expired.setWindowStartedAt(dbNow.minusHours(2));
        expired.setSendCount(1);
        verifications.saveCode(expired);
        assertEquals(0, verifications.consume(expiredEmail, expiredHash),
                "已过期的验证码不能被消费");

        String lockedEmail = unique("mapperxmlverify-locked") + "@example.test";
        verifications.ensureEmail(lockedEmail);
        String lockedHash = token64(unique("mapperxmlcode-locked"));
        EmailVerificationEntity locked = new EmailVerificationEntity();
        locked.setEmail(lockedEmail);
        locked.setCodeHash(lockedHash);
        locked.setExpiresAt(dbNow.plusHours(1));
        locked.setSentAt(dbNow);
        locked.setWindowStartedAt(dbNow);
        locked.setSendCount(1);
        verifications.saveCode(locked);
        for (int attempt = 0; attempt < 5; attempt++) {
            verifications.incrementFailures(lockedEmail);
        }
        assertEquals(5, verifications.findForUpdate(lockedEmail).getFailedAttempts());
        assertEquals(0, verifications.consume(lockedEmail, lockedHash),
                "failed_attempts >= 5 后即使验证码正确也不能消费");
        assertNull(verifications.findForUpdate(lockedEmail).getConsumedAt());

        // 重新发码会把失败次数清零，重新允许消费
        locked.setCodeHash(token64(unique("mapperxmlcode-reissued")));
        locked.setSentAt(dbNow);
        locked.setSendCount(2);
        verifications.saveCode(locked);
        assertEquals(1, verifications.consume(lockedEmail, locked.getCodeHash()));
    }

    @Test
    void rollbackLeavesNoAccountProbeRowsBehind() {
        AccountEntity probe = newAccount("ACTIVE");
        accounts.insertAccount(probe);
        Long id = probe.getId();
        assertNotNull(id);

        EmailVerificationEntity verification = new EmailVerificationEntity();
        verification.setEmail(probe.getEmail());
        verifications.saveCode(verification);
        String verifiedEmail = probe.getEmail();

        session.rollback();
        try (SqlSession fresh = openSession()) {
            assertNull(fresh.getMapper(AccountMapper.class).accountById(id),
                    "回滚后不应在开发库里留下测试账户");
            assertNull(fresh.getMapper(EmailVerificationMapper.class).findForUpdate(verifiedEmail),
                    "回滚后不应在开发库里留下测试邮箱验证行");
        }
    }

    private AccountEntity newAccount(String status) {
        String marker = unique("mapperxmlacct");
        AccountEntity account = new AccountEntity();
        account.setUsername(marker);
        account.setEmail(marker + "@example.test");
        account.setDisplayName("XML 验证账户");
        account.setStatus(status);
        account.setEmailVerifiedAt(null);
        return account;
    }

    private AdminInvitationEntity newInvitation(long targetAccountId, String targetEmail, String hash,
                                                long invitedBy, LocalDateTime expiresAt) {
        AdminInvitationEntity invitation = new AdminInvitationEntity();
        invitation.setTargetAccountId(targetAccountId);
        invitation.setEmailSnapshot(targetEmail);
        invitation.setTargetRoleCode("ADMIN");
        invitation.setTokenHash(hash);
        invitation.setStatus("PENDING");
        invitation.setInvitedBy(invitedBy);
        invitation.setExpiresAt(expiresAt);
        return invitation;
    }

    private PasswordResetEntity newReset(long accountId, String hash, LocalDateTime expiresAt) {
        PasswordResetEntity reset = new PasswordResetEntity();
        reset.setAccountId(accountId);
        reset.setTokenHash(hash);
        reset.setStatus("PENDING");
        reset.setExpiresAt(expiresAt);
        return reset;
    }

    private AccountAuditEntity newAudit(long actorId, Long targetId, String action, String result, String ip) {
        AccountAuditEntity audit = new AccountAuditEntity();
        audit.setActorAccountId(actorId);
        audit.setTargetAccountId(targetId);
        audit.setActionCode(action);
        audit.setResult(result);
        audit.setIpAddress(ip);
        audit.setUserAgent("mapper-xml-test-agent");
        return audit;
    }

    /* invitationColumns 没有 selected accepted_by，只能直接读库核对 acceptInvitation 的写路径 */
    private long acceptedById(long invitationId) {
        try (PreparedStatement statement = session.getConnection().prepareStatement(
                "SELECT accepted_by FROM sr_admin_invitation WHERE id = ?")) {
            statement.setLong(1, invitationId);
            try (ResultSet rows = statement.executeQuery()) {
                assertTrue(rows.next(), "邀请行不见了");
                return rows.getLong(1);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("读取 accepted_by 失败", exception);
        }
    }

    /* token_hash 是 char(64)，拿足 64 个字符避免被空格补齐后与断言里的字符串不等 */    private String token64(String seed) {
        String cleaned = seed.replaceAll("[^A-Za-z0-9]", "");
        StringBuilder builder = new StringBuilder();
        while (builder.length() < 64) {
            builder.append(cleaned);
        }
        return builder.substring(0, 64);
    }

    private String unique(String prefix) {
        return prefix + "-" + PROBE.incrementAndGet() + "-" + System.nanoTime();
    }

    /* 账户模块的业务时间一律是 UTC（见类注释里的时区说明），这里与生产 Service 保持同一口径 */
    private LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
