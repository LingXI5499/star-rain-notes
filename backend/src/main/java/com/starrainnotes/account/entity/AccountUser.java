package com.starrainnotes.account.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("user_account")
public class AccountUser {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String email;
    private String passwordHash;
    private String role;
    private String accountStatus;
    private LocalDateTime emailVerifiedAt;
    private LocalDateTime activatedAt;
    private Integer failedLoginCount;
    private LocalDateTime lockedUntil;
    private Integer authVersion;
    private LocalDateTime lastLoginAt;
    private LocalDateTime passwordChangedAt;
    private LocalDateTime disabledAt;
    private Long disabledBy;
    private String disabledReason;
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getAccountStatus() { return accountStatus; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }
    public LocalDateTime getEmailVerifiedAt() { return emailVerifiedAt; }
    public void setEmailVerifiedAt(LocalDateTime v) { this.emailVerifiedAt = v; }
    public LocalDateTime getActivatedAt() { return activatedAt; }
    public void setActivatedAt(LocalDateTime v) { this.activatedAt = v; }
    public Integer getFailedLoginCount() { return failedLoginCount; }
    public void setFailedLoginCount(Integer v) { this.failedLoginCount = v; }
    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime v) { this.lockedUntil = v; }
    public Integer getAuthVersion() { return authVersion; }
    public void setAuthVersion(Integer v) { this.authVersion = v; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime v) { this.lastLoginAt = v; }
    public LocalDateTime getPasswordChangedAt() { return passwordChangedAt; }
    public void setPasswordChangedAt(LocalDateTime v) { this.passwordChangedAt = v; }
    public LocalDateTime getDisabledAt() { return disabledAt; }
    public void setDisabledAt(LocalDateTime v) { this.disabledAt = v; }
    public Long getDisabledBy() { return disabledBy; }
    public void setDisabledBy(Long v) { this.disabledBy = v; }
    public String getDisabledReason() { return disabledReason; }
    public void setDisabledReason(String v) { this.disabledReason = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
}