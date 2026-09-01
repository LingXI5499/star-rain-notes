package com.starrainnotes.profile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * Profile selected content (frozen table {@code profile_selected_content}).
 * Exactly one of the three business FKs is set per row (DB CHECK); content
 * deletion cascades (DB FK).
 */
@TableName("profile_selected_content")
public class ProfileSelectedContent {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer profileId;
    private Long tutorialId;
    private Long blogPostId;
    private Long portfolioProjectId;
    private Integer sortOrder;
    @TableField(updateStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.NEVER)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getProfileId() {
        return profileId;
    }

    public void setProfileId(Integer profileId) {
        this.profileId = profileId;
    }

    public Long getTutorialId() {
        return tutorialId;
    }

    public void setTutorialId(Long tutorialId) {
        this.tutorialId = tutorialId;
    }

    public Long getBlogPostId() {
        return blogPostId;
    }

    public void setBlogPostId(Long blogPostId) {
        this.blogPostId = blogPostId;
    }

    public Long getPortfolioProjectId() {
        return portfolioProjectId;
    }

    public void setPortfolioProjectId(Long portfolioProjectId) {
        this.portfolioProjectId = portfolioProjectId;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
