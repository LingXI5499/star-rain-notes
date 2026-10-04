package com.starrainnotes.profile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sr_profile_social_link")
public class SocialLinkEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long profileId;
    private String platformCode;
    private String label;
    private String url;
    private Integer sortOrder;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
