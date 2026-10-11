/*
 * ReviewTargetView：审核模块的跨模块接口文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 * 阅读入口：viewType（view类型处理入口）。
 */

package com.starrainnotes.review.api;

/*
 * 目标业务模块提供的「不可变审核视图」。
 *
 * Review 刻意不认识 TutorialEntity / BlogPostEntity：它只把这个接口原样
 * 放进审核详情响应里。业务模块自己决定暴露哪些字段用于审核，
 * Review 不解析也不校验其中的业务规则。
 *
 * 实现类示例：TutorialReviewVO、BlogReviewView。
 */
/**
 * 审核目标View，属于审核模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
public interface ReviewTargetView {

    // 视图类型标识，例如 TUTORIAL_CHAPTER；前端据此选择渲染方式
    /**
     * view类型处理入口。
     * @return 类型为 <code>String</code> 的处理结果
     */
    String viewType();
}
