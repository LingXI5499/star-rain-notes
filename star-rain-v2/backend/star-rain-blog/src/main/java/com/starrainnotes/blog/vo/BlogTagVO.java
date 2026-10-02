package com.starrainnotes.blog.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 标签视图。
 *
 * postCount 是当前绑定该标签的文章数（含草稿与已撤回）：
 * 后台据此判断“这个标签还在被使用”，从而不提供物理删除入口。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogTagVO {

    private Long id;
    private String slug;
    private String name;
    private String description;
    private String status;
    private Long postCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
