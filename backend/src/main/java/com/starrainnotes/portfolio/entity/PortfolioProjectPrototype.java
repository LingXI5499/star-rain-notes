package com.starrainnotes.portfolio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("portfolio_project_prototype")
public class PortfolioProjectPrototype {
    @TableId(type = IdType.AUTO) private Long id;
    private Long projectId;
    private Long mediaAssetId;
    private String revision;
    private String entryPath;
    private String storageKey;
    private String sourceName;
    private Integer fileCount;
    private Long totalBytes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    public Long getId() { return id; } public void setId(Long v) { id = v; }
    public Long getProjectId() { return projectId; } public void setProjectId(Long v) { projectId = v; }
    public Long getMediaAssetId() { return mediaAssetId; } public void setMediaAssetId(Long v) { mediaAssetId = v; }
    public String getRevision() { return revision; } public void setRevision(String v) { revision = v; }
    public String getEntryPath() { return entryPath; } public void setEntryPath(String v) { entryPath = v; }
    public String getStorageKey() { return storageKey; } public void setStorageKey(String v) { storageKey = v; }
    public String getSourceName() { return sourceName; } public void setSourceName(String v) { sourceName = v; }
    public Integer getFileCount() { return fileCount; } public void setFileCount(Integer v) { fileCount = v; }
    public Long getTotalBytes() { return totalBytes; } public void setTotalBytes(Long v) { totalBytes = v; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime v) { createdAt = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime v) { updatedAt = v; }
}
