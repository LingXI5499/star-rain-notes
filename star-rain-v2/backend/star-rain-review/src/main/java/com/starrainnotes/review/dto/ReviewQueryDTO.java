package com.starrainnotes.review.dto;

import lombok.Data;

/*
 * REV-002 待审核列表查询条件。
 *
 * 与 Media 模块同一口径：刻意不加 jakarta.validation 注解，
 * 查询参数合法性由 Service 显式校验并返回 REVIEW_QUERY_INVALID，
 * 这样错误码可控、可测，也不依赖 @ModelAttribute 的绑定异常类型。
 */
@Data
public class ReviewQueryDTO {

    private int page = 1;

    private int pageSize = 20;

    // 目标模块，如 TUTORIAL；空表示不筛选
    private String targetModule;

    // 审核类型，如 tutorial.publish；空表示不筛选
    private String reviewType;

    // 关键字：匹配目标显示名称与申请人显示名称
    private String keyword;
}
