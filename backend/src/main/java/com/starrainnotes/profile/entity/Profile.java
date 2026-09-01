package com.starrainnotes.profile.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Profile singleton (frozen table {@code profile}, id = 1).
 */
@TableName(value = "profile", autoResultMap = true)
public class Profile {

    @TableId(type = IdType.INPUT)
    private Integer id;
    private String displayName;
    private String headline;
    private String bio;
    private Long avatarMediaId;
    private String githubUrl;
    private String publicEmail;
    private Long resumeMediaId;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> currentFocus;
    private String technicalDirectionMarkdown;
    private String journeyMarkdown;
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public Long getAvatarMediaId() {
        return avatarMediaId;
    }

    public void setAvatarMediaId(Long avatarMediaId) {
        this.avatarMediaId = avatarMediaId;
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    public void setGithubUrl(String githubUrl) {
        this.githubUrl = githubUrl;
    }

    public String getPublicEmail() {
        return publicEmail;
    }

    public void setPublicEmail(String publicEmail) {
        this.publicEmail = publicEmail;
    }

    public Long getResumeMediaId() {
        return resumeMediaId;
    }

    public void setResumeMediaId(Long resumeMediaId) {
        this.resumeMediaId = resumeMediaId;
    }

    public List<String> getCurrentFocus() {
        return currentFocus;
    }

    public void setCurrentFocus(List<String> currentFocus) {
        this.currentFocus = currentFocus;
    }

    public String getTechnicalDirectionMarkdown() {
        return technicalDirectionMarkdown;
    }

    public void setTechnicalDirectionMarkdown(String technicalDirectionMarkdown) {
        this.technicalDirectionMarkdown = technicalDirectionMarkdown;
    }

    public String getJourneyMarkdown() {
        return journeyMarkdown;
    }

    public void setJourneyMarkdown(String journeyMarkdown) {
        this.journeyMarkdown = journeyMarkdown;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
