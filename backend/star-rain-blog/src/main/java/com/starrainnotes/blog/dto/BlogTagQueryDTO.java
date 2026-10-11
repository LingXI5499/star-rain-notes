package com.starrainnotes.blog.dto;

import lombok.Data;

// 后台标签列表查询条件
@Data
public class BlogTagQueryDTO {

    private int page = 1;
    private int pageSize = 20;

    // slug 与 name 模糊匹配
    private String keyword;

    // ENABLED / DISABLED，空白表示全部
    private String status;
}
