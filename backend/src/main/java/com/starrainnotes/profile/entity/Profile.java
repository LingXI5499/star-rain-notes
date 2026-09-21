package com.starrainnotes.profile.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/** Profile singleton (frozen table {@code profile}, id = 1). */
@Getter
@Setter
@NoArgsConstructor
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
}
