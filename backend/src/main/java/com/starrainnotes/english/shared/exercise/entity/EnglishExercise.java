package com.starrainnotes.english.shared.exercise.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Base exercise row (frozen table {@code english_exercise}). Per-content
 * association is done via module-specific mapping tables (reading/listening/
 * writing) that carry real FKs; this table holds the shared exercise payload.
 * {@code configJson} must be validated per question type at the service layer.
 */
@TableName(value = "english_exercise", autoResultMap = true)
public class EnglishExercise {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String moduleType;
    private String questionType;
    private String promptMarkdown;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> configJson;
    private String explanationMarkdown;
    private Integer scoreValue;
    private Integer sortOrder;
    private String publishStatus;
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getModuleType() {
        return moduleType;
    }

    public void setModuleType(String moduleType) {
        this.moduleType = moduleType;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public String getPromptMarkdown() {
        return promptMarkdown;
    }

    public void setPromptMarkdown(String promptMarkdown) {
        this.promptMarkdown = promptMarkdown;
    }

    public Map<String, Object> getConfigJson() {
        return configJson;
    }

    public void setConfigJson(Map<String, Object> configJson) {
        this.configJson = configJson;
    }

    public String getExplanationMarkdown() {
        return explanationMarkdown;
    }

    public void setExplanationMarkdown(String explanationMarkdown) {
        this.explanationMarkdown = explanationMarkdown;
    }

    public Integer getScoreValue() {
        return scoreValue;
    }

    public void setScoreValue(Integer scoreValue) {
        this.scoreValue = scoreValue;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getPublishStatus() {
        return publishStatus;
    }

    public void setPublishStatus(String publishStatus) {
        this.publishStatus = publishStatus;
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
