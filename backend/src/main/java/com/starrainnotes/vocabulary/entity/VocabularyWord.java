package com.starrainnotes.vocabulary.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * One vocabulary entry (approved table {@code vocabulary_word}).
 *
 * <p>{@code examples} is a JSON array of {@link VocabularyExample} added by
 * the admin; {@code memory_count} / {@code last_memory_at} are the personal
 * memory record — incremented manually from the public card (+1) and
 * correctable from the admin panel.</p>
 */
@TableName(value = "vocabulary_word", autoResultMap = true)
public class VocabularyWord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long themeId;
    private String partOfSpeech;
    private String word;
    private String phoneticUs;
    private String translation;
    private String inflections;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<VocabularyExample> examples = new ArrayList<>();
    private Integer memoryCount;
    private LocalDateTime lastMemoryAt;
    private Integer sortOrder;
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

    public Long getThemeId() {
        return themeId;
    }

    public void setThemeId(Long themeId) {
        this.themeId = themeId;
    }

    public String getPartOfSpeech() {
        return partOfSpeech;
    }

    public void setPartOfSpeech(String partOfSpeech) {
        this.partOfSpeech = partOfSpeech;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getPhoneticUs() {
        return phoneticUs;
    }

    public void setPhoneticUs(String phoneticUs) {
        this.phoneticUs = phoneticUs;
    }

    public String getTranslation() {
        return translation;
    }

    public void setTranslation(String translation) {
        this.translation = translation;
    }

    public String getInflections() {
        return inflections;
    }

    public void setInflections(String inflections) {
        this.inflections = inflections;
    }

    public List<VocabularyExample> getExamples() {
        return examples;
    }

    public void setExamples(List<VocabularyExample> examples) {
        this.examples = examples == null ? new ArrayList<>() : examples;
    }

    public Integer getMemoryCount() {
        return memoryCount;
    }

    public void setMemoryCount(Integer memoryCount) {
        this.memoryCount = memoryCount;
    }

    public LocalDateTime getLastMemoryAt() {
        return lastMemoryAt;
    }

    public void setLastMemoryAt(LocalDateTime lastMemoryAt) {
        this.lastMemoryAt = lastMemoryAt;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
