package com.starrainnotes.site.section;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sr_site_home_section")
public class HomeSectionEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private String sectionCode;
    private String displayName;
    private Boolean enabled;
    private Integer sortOrder;
    private String configJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
