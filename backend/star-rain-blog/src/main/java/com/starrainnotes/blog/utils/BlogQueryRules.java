/*
 * BlogQueryRules：博客模块的工具文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 * 主要协作类型：BlogPostStatus、BlogTaxonomyStatus、BlogQueryInvalidException。
 * 阅读入口：validatePage（校验分页）；postStatus（文章状态处理入口）；taxonomyStatus（taxonomy状态处理入口）；optionalId（optional标识处理入口）。
 */

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
/**
 * 博客查询参数规则，属于博客模块。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 */
public final class BlogQueryRules {

    /**
     * 最大。
     * 初始值为 <code>100</code>。
     */
    public static final int PAGE_SIZE_MAX = 100;
    /**
     * 最大。
     * 初始值为 <code>10000</code>。
     */
    public static final int PAGE_MAX = 10_000;

    // 归档年份的合理范围：早于 2000 或晚于下一年都说明参数写错了
    /**
     * 最小。
     * 初始值为 <code>2000</code>。
     */
    private static final int YEAR_MIN = 2000;

    /**
     * 构造BlogQueryRules，接收本类使用的依赖或初始化参数。
     */
    private BlogQueryRules() {
    }

    /**
     * 校验分页。
     * 实现中的显式失败类型包括 <code>BlogQueryInvalidException</code>；具体触发条件见对应分支。
     * 边界判断包含 <code>(page &lt; 1)</code>；<code>(page &gt; PAGE_MAX)</code>。
     *
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     */
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
    /**
     * 文章状态处理入口。
     * 关键处理步骤调用 <code>value.isBlank</code>、<code>BlogPostStatus.of</code>。
     * 实现中的显式失败类型包括 <code>BlogQueryInvalidException</code>；具体触发条件见对应分支。
     * 边界判断包含 <code>(value == null || value.isBlank())</code>；<code>(status == null)</code>。
     *
     * @param value 值；类型为 <code>String</code>
     * @return 类型为 <code>BlogPostStatus</code> 的处理结果
     */
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

    /**
     * taxonomy状态处理入口。
     * 关键处理步骤调用 <code>value.isBlank</code>、<code>BlogTaxonomyStatus.of</code>。
     * 实现中的显式失败类型包括 <code>BlogQueryInvalidException</code>；具体触发条件见对应分支。
     * 边界判断包含 <code>(value == null || value.isBlank())</code>；<code>(status == null)</code>。
     *
     * @param value 值；类型为 <code>String</code>
     * @return 类型为 <code>BlogTaxonomyStatus</code> 的处理结果
     */
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
    /**
     * optional标识处理入口。
     * 实现中的显式失败类型包括 <code>BlogQueryInvalidException</code>；具体触发条件见对应分支。
     * 边界判断包含 <code>(value == null)</code>；<code>(value &lt;= 0)</code>。
     *
     * @param value 值；类型为 <code>Long</code>
     * @param fieldName 字段名称；类型为 <code>String</code>
     * @return 类型为 <code>Long</code> 的处理结果
     */
    public static Long optionalId(Long value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value <= 0) {
            throw new BlogQueryInvalidException(fieldName + " 必须为正整数");
        }
        return value;
    }

    /**
     * archive年份处理入口。
     * 关键处理步骤调用 <code>java.time.LocalDate.now</code>。
     * 实现中的显式失败类型包括 <code>BlogQueryInvalidException</code>；具体触发条件见对应分支。
     * 边界判断包含 <code>(value == null)</code>；<code>(value &lt; YEAR_MIN || value &gt; nextYear)</code>。
     *
     * @param value 值；类型为 <code>Integer</code>
     * @return 类型为 <code>Integer</code> 的处理结果
     */
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
    /**
     * archive月份处理入口。
     * 实现中的显式失败类型包括 <code>BlogQueryInvalidException</code>；具体触发条件见对应分支。
     * 边界判断包含 <code>(month == null)</code>；<code>(year == null)</code>。
     *
     * @param year 年份；类型为 <code>Integer</code>
     * @param month 月份；类型为 <code>Integer</code>
     * @return 类型为 <code>Integer</code> 的处理结果
     */
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

    /**
     * archive日处理入口。
     * 关键处理步骤调用 <code>java.time.YearMonth.of</code>。
     * 实现中的显式失败类型包括 <code>BlogQueryInvalidException</code>；具体触发条件见对应分支。
     * 边界判断包含 <code>(day == null)</code>；<code>(year == null || month == null)</code>。
     *
     * @param year 年份；类型为 <code>Integer</code>
     * @param month 月份；类型为 <code>Integer</code>
     * @param day 日；类型为 <code>Integer</code>
     * @return 类型为 <code>Integer</code> 的处理结果
     */
    public static Integer archiveDay(Integer year, Integer month, Integer day) {
        if (day == null) {
            return null;
        }
        if (year == null || month == null) {
            throw new BlogQueryInvalidException("按天归档时必须同时给出 year 和 month");
        }
        if (day < 1 || day > java.time.YearMonth.of(year, month).lengthOfMonth()) {
            throw new BlogQueryInvalidException("day 不在所选月份内");
        }
        return day;
    }

    /**
     * 查询跳过的记录数处理入口。
     *
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @return 类型为 <code>int</code> 的处理结果
     */
    public static int offset(int page, int pageSize) {
        return (page - 1) * pageSize;
    }
}
