package com.starrainnotes.blog.dto;

import lombok.Data;

// 后台专题列表查询条件
@Data
public class BlogTopicQueryDTO {

    private int page = 1;
    private int pageSize = 20;

    private String keyword;

    // ENABLED / DISABLED，空白表示全部
    private String status;
}
