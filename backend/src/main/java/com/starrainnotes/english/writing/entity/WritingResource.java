package com.starrainnotes.english.writing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("english_writing_resource")
public class WritingResource {
    @TableId(type = IdType.AUTO) private Long id;
    private String resourceKind;
    private String expressionLevel;
    private String title;
    private String slug;
    private String summary;
    private String bodyMarkdown;
    private Long coverMediaId;
    private String cefrLevel;
    private Integer wordMin;
    private Integer wordMax;
    private Integer estimatedMinutes;
    private String templateSchemaJson;
    private String publishStatus;
    private Integer sortOrder;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public String getResourceKind(){return resourceKind;} public void setResourceKind(String v){resourceKind=v;}
    public String getExpressionLevel(){return expressionLevel;} public void setExpressionLevel(String v){expressionLevel=v;}
    public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public String getSlug(){return slug;} public void setSlug(String v){slug=v;}
    public String getSummary(){return summary;} public void setSummary(String v){summary=v;}
    public String getBodyMarkdown(){return bodyMarkdown;} public void setBodyMarkdown(String v){bodyMarkdown=v;}
    public Long getCoverMediaId(){return coverMediaId;} public void setCoverMediaId(Long v){coverMediaId=v;}
    public String getCefrLevel(){return cefrLevel;} public void setCefrLevel(String v){cefrLevel=v;}
    public Integer getWordMin(){return wordMin;} public void setWordMin(Integer v){wordMin=v;}
    public Integer getWordMax(){return wordMax;} public void setWordMax(Integer v){wordMax=v;}
    public Integer getEstimatedMinutes(){return estimatedMinutes;} public void setEstimatedMinutes(Integer v){estimatedMinutes=v;}
    public String getTemplateSchemaJson(){return templateSchemaJson;} public void setTemplateSchemaJson(String v){templateSchemaJson=v;}
    public String getPublishStatus(){return publishStatus;} public void setPublishStatus(String v){publishStatus=v;}
    public Integer getSortOrder(){return sortOrder;} public void setSortOrder(Integer v){sortOrder=v;}
    public LocalDateTime getPublishedAt(){return publishedAt;} public void setPublishedAt(LocalDateTime v){publishedAt=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime v){updatedAt=v;}
}
