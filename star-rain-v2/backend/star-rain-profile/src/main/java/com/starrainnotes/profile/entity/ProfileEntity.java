package com.starrainnotes.profile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sr_profile")
public class ProfileEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private String profileKey;
    private String displayName;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private String headline;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private String bioMarkdown;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private String locationText;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private Long avatarMediaAssetId;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private Long resumeMediaAssetId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
