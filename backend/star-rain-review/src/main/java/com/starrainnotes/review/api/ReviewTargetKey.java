/*
 * ReviewTargetKey：审核模块的跨模块接口文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */

package com.starrainnotes.review.api;

/*
 * 目标业务对象的定位三元组。
 *
 * 目标模块用它表达「我要提交/取消/查询哪一个对象」，
 * Review 用它做同一目标只能有一个 PENDING 的判定。
 */
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 审核目标键，属于审核模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewTargetKey {

    /**
     * 目标对象所属模块。
     */
    private String targetModule;
    /**
     * 目标对象的业务类型。
     */
    private String targetType;
    /**
     * 被处理或审核的目标对象标识。
     */
    private Long targetId;
}
