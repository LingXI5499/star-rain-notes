package com.starrainnotes.tutorial.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sr_tutorial")
public class TutorialEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long categoryId;
    private String slug;
    private String title;
    private String summary;
    private Integer sortOrder;
    private Long coverMediaAssetId;
    private String publicationStatus;
    private String editingStatus;
    private Long publishedRevisionId;
    private LocalDateTime publishedAt;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime withdrawnAt;
    private Long createdByAccountId;
    private Long updatedByAccountId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
