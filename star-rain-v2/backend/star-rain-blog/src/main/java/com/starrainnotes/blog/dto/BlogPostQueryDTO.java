package com.starrainnotes.blog.dto;

import lombok.Data;

/*
 * 后台文章列表查询条件。
 *
 * 查询接口不写校验注解：page / pageSize / status 的合法性由 Service 显式校验，
 * 越界分页返回 BLOG_QUERY_INVALID，而不是让数据库报错后变成 500。
 */
@Data
public class BlogPostQueryDTO {

    private int page = 1;
    private int pageSize = 20;

    // 标题与 slug 模糊匹配
    private String keyword;

    // DRAFT / PUBLISHED / WITHDRAWN，空白表示全部
    private String status;

    private Long tagId;
    private Long topicId;
}
