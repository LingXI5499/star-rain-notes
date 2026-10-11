/*
 * ReviewQueryDTO：审核模块的请求与传输数据文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.review.dto;

import lombok.Data;

/*
 * REV-002 待审核列表查询条件。
 *
 * 与 Media 模块同一口径：刻意不加 jakarta.validation 注解，
 * 查询参数合法性由 Service 显式校验并返回 REVIEW_QUERY_INVALID，
 * 这样错误码可控、可测，也不依赖 @ModelAttribute 的绑定异常类型。
 */
/**
 * 审核查询参数传输对象，属于审核模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data
public class ReviewQueryDTO {

    /**
     * 分页页码。
     * 初始值为 <code>1</code>。
     */
    private int page = 1;

    /**
     * 单页返回条目数。
     * 初始值为 <code>20</code>。
     */
    private int pageSize = 20;

    // 目标模块，如 TUTORIAL；空表示不筛选
    /**
     * 目标对象所属模块。
     */
    private String targetModule;

    // 审核类型，如 tutorial.publish；空表示不筛选
    /**
     * 审核类型。
     */
    private String reviewType;

    // 关键字：匹配目标显示名称与申请人显示名称
    /**
     * 关键词。
     */
    private String keyword;
}
