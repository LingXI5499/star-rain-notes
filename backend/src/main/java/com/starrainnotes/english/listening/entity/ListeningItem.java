package com.starrainnotes.english.listening.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * Listening material (frozen table {@code english_listening_item}). The backend
 * validates the audio media is an AUDIO asset and the cover is an IMAGE asset;
 * both happen at the service layer per 方案 §7.2 / 阶段三 §三.
 */
@TableName("english_listening_item")
public class ListeningItem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String slug;
    private String summary;
    private String transcriptMarkdown;
    private String cefrLevel;
    private Integer listeningLevel;
    private Long audioMediaId;
    private Long coverMediaId;
    private Integer durationSeconds;
    private String sourceName;
    private String sourceUrl;
    private String copyrightNote;
    private String publishStatus;
    private Integer sortOrder;
    private LocalDateTime publishedAt;
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getTranscriptMarkdown() { return transcriptMarkdown; }
    public void setTranscriptMarkdown(String transcriptMarkdown) { this.transcriptMarkdown = transcriptMarkdown; }
    public String getCefrLevel() { return cefrLevel; }
    public void setCefrLevel(String cefrLevel) { this.cefrLevel = cefrLevel; }
    public Integer getListeningLevel() { return listeningLevel; }
    public void setListeningLevel(Integer listeningLevel) { this.listeningLevel = listeningLevel; }
    public Long getAudioMediaId() { return audioMediaId; }
    public void setAudioMediaId(Long audioMediaId) { this.audioMediaId = audioMediaId; }
    public Long getCoverMediaId() { return coverMediaId; }
    public void setCoverMediaId(Long coverMediaId) { this.coverMediaId = coverMediaId; }
    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public String getCopyrightNote() { return copyrightNote; }
    public void setCopyrightNote(String copyrightNote) { this.copyrightNote = copyrightNote; }
    public String getPublishStatus() { return publishStatus; }
    public void setPublishStatus(String publishStatus) { this.publishStatus = publishStatus; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
