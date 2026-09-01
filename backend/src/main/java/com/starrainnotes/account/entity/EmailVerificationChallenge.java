package com.starrainnotes.account.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("email_verification_challenge")
public class EmailVerificationChallenge {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String email;
    private String purpose;
    private Long invitationId;
    private String codeHash;
    private Integer attemptCount;
    private LocalDateTime expiresAt;
    private LocalDateTime consumedAt;
    private String requestIpHash;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String v) { this.purpose = v; }
    public Long getInvitationId() { return invitationId; }
    public void setInvitationId(Long v) { this.invitationId = v; }
    public String getCodeHash() { return codeHash; }
    public void setCodeHash(String v) { this.codeHash = v; }
    public Integer getAttemptCount() { return attemptCount; }
    public void setAttemptCount(Integer v) { this.attemptCount = v; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime v) { this.expiresAt = v; }
    public LocalDateTime getConsumedAt() { return consumedAt; }
    public void setConsumedAt(LocalDateTime v) { this.consumedAt = v; }
    public String getRequestIpHash() { return requestIpHash; }
    public void setRequestIpHash(String v) { this.requestIpHash = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
}