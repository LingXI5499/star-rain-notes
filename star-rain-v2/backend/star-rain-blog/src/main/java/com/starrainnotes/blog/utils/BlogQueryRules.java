package com.starrainnotes.blog.utils;

import com.starrainnotes.blog.enumeration.BlogPostStatus;
import com.starrainnotes.blog.enumeration.BlogTaxonomyStatus;
import com.starrainnotes.blog.exception.BlogQueryInvalidException;

/*
 * 查询与分页参数的显式校验。
 *
 * 查询接口刻意不写 jakarta.validation 注解，合法性都在这里判断：
 * 这样越界分页、非法状态值都会得到一个明确且稳定的 BLOG_QUERY_INVALID，
 * 而不是变成 MySQL 的语法/范围错误（500），或者被全局处理器糊成 INVALID_REQUEST。
 */
public final class BlogQueryRules {

    public static final int PAGE_SIZE_MAX = 100;
    public static final int PAGE_MAX = 10_000;

    // 归档年份的合理范围：早于 2000 或晚于下一年都说明参数写错了
    private static final int YEAR_MIN = 2000;

    private BlogQueryRules() {
    }

    public static void validatePage(int page, int pageSize) {
        if (page < 1) {
            throw new BlogQueryInvalidException("page 必须大于等于 1");
        }
        if (page > PAGE_MAX) {
            throw new BlogQueryInvalidException("page 超出允许范围");
        }
        if (pageSize < 1 || pageSize > PAGE_SIZE_MAX) {
            throw new BlogQueryInvalidException("pageSize 必须在 1 到 " + PAGE_SIZE_MAX + " 之间");
        }
    }

    // 空白表示不筛选；非法值明确报错，不静默忽略（静默忽略会让前端以为筛选生效了）
    public static BlogPostStatus postStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        BlogPostStatus status = BlogPostStatus.of(value);
        if (status == null) {
            throw new BlogQueryInvalidException("status 只能是 DRAFT/PUBLISHED/WITHDRAWN");
        }
        return status;
    }

    public static BlogTaxonomyStatus taxonomyStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        BlogTaxonomyStatus status = BlogTaxonomyStatus.of(value);
        if (status == null) {
            throw new BlogQueryInvalidException("status 只能是 ENABLED/DISABLED");
        }
        return status;
    }

    // 主键筛选条件：0 或负数一律视为写错，而不是“查不到”
    public static Long optionalId(Long value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value <= 0) {
            throw new BlogQueryInvalidException(fieldName + " 必须为正整数");
        }
        return value;
    }

    public static Integer archiveYear(Integer value) {
        if (value == null) {
            return null;
        }
        int nextYear = java.time.LocalDate.now().getYear() + 1;
        if (value < YEAR_MIN || value > nextYear) {
            throw new BlogQueryInvalidException("year 必须在 " + YEAR_MIN + " 到 " + nextYear + " 之间");
        }
        return value;
    }

    // month 只有在给出 year 时才有意义，单独给 month 视为参数不完整
    public static Integer archiveMonth(Integer year, Integer month) {
        if (month == null) {
            return null;
        }
        if (year == null) {
            throw new BlogQueryInvalidException("按月归档时必须同时给出 year");
        }
        if (month < 1 || month > 12) {
            throw new BlogQueryInvalidException("month 必须在 1 到 12 之间");
        }
        return month;
    }

    public static int offset(int page, int pageSize) {
        return (page - 1) * pageSize;
    }
}
