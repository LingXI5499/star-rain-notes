package com.starrainnotes.account.audit;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;

import java.time.LocalDateTime;
import java.util.Map;

@TableName(value = "admin_audit_log", autoResultMap = true)
public class AuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long actorId;
    private String action;
    private String targetType;
    private Long targetId;
    private String result;
    private String requestIpHash;
    private String userAgentSummary;
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private Map<String, Object> metadataJson;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getActorId() { return actorId; }
    public void setActorId(Long v) { this.actorId = v; }
    public String getAction() { return action; }
    public void setAction(String v) { this.action = v; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String v) { this.targetType = v; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long v) { this.targetId = v; }
    public String getResult() { return result; }
    public void setResult(String v) { this.result = v; }
    public String getRequestIpHash() { return requestIpHash; }
    public void setRequestIpHash(String v) { this.requestIpHash = v; }
    public String getUserAgentSummary() { return userAgentSummary; }
    public void setUserAgentSummary(String v) { this.userAgentSummary = v; }
    public Map<String, Object> getMetadataJson() { return metadataJson; }
    public void setMetadataJson(Map<String, Object> v) { this.metadataJson = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
}