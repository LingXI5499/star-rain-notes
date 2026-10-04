package com.starrainnotes.site.config;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sr_site_config")
public class SiteConfigEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private String configKey;
    private String siteName;
    private String siteTitle;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private String siteDescription;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private String homeIntro;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private String footerText;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private Long logoMediaAssetId;
    @TableField(updateStrategy = FieldStrategy.ALWAYS) private Long faviconMediaAssetId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
