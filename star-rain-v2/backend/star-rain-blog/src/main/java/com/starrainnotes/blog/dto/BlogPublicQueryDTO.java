package com.starrainnotes.blog.dto;

import lombok.Data;

/*
 * BLOG-001 / BLOG-010 公开查询条件：列表与归档共用一个 DTO。
 *
 * year / month 只对归档有意义，列表接口忽略它们；
 * 共用一个 DTO 是为了让“组合筛选”的合法性校验只有一处实现。
 */
@Data
public class BlogPublicQueryDTO {

    private int page = 1;
    private int pageSize = 20;

    // Tag slug 与 Topic slug，都走 slug 而不是 ID：公开 URL 不暴露内部主键
    private String tag;
    private String topic;

    // 归档时间维度，依据 publishedAt 而不是 createdAt
    private Integer year;
    private Integer month;
    private Integer day;
}
