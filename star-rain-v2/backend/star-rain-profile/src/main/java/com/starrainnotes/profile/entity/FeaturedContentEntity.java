package com.starrainnotes.profile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sr_profile_featured_content")
public class FeaturedContentEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long profileId;
    private String contentType;
    private Long contentId;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private String titleOverride;
    private Integer sortOrder;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
