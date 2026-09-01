package com.starrainnotes.english.shared.cefr.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * CEFR standard level (frozen table {@code english_cefr_standard}).
 * Primary key is the level code A1–C2. Range upper bounds may be {@code null};
 * non-null values are guaranteed non-negative with min ≤ max by the DB CHECK.
 */
@TableName("english_cefr_standard")
public class EnglishCefrStandard {

    @TableId(type = IdType.INPUT)
    private String level;
    private Integer vocabMin;
    private Integer vocabMax;
    private Integer readingSentenceMin;
    private Integer readingSentenceMax;
    private Integer listeningWpmMin;
    private Integer listeningWpmMax;
    private Integer writingLengthMin;
    private Integer writingLengthMax;
    private String description;
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public Integer getVocabMin() {
        return vocabMin;
    }

    public void setVocabMin(Integer vocabMin) {
        this.vocabMin = vocabMin;
    }

    public Integer getVocabMax() {
        return vocabMax;
    }

    public void setVocabMax(Integer vocabMax) {
        this.vocabMax = vocabMax;
    }

    public Integer getReadingSentenceMin() {
        return readingSentenceMin;
    }

    public void setReadingSentenceMin(Integer readingSentenceMin) {
        this.readingSentenceMin = readingSentenceMin;
    }

    public Integer getReadingSentenceMax() {
        return readingSentenceMax;
    }

    public void setReadingSentenceMax(Integer readingSentenceMax) {
        this.readingSentenceMax = readingSentenceMax;
    }

    public Integer getListeningWpmMin() {
        return listeningWpmMin;
    }

    public void setListeningWpmMin(Integer listeningWpmMin) {
        this.listeningWpmMin = listeningWpmMin;
    }

    public Integer getListeningWpmMax() {
        return listeningWpmMax;
    }

    public void setListeningWpmMax(Integer listeningWpmMax) {
        this.listeningWpmMax = listeningWpmMax;
    }

    public Integer getWritingLengthMin() {
        return writingLengthMin;
    }

    public void setWritingLengthMin(Integer writingLengthMin) {
        this.writingLengthMin = writingLengthMin;
    }

    public Integer getWritingLengthMax() {
        return writingLengthMax;
    }

    public void setWritingLengthMax(Integer writingLengthMax) {
        this.writingLengthMax = writingLengthMax;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
