package com.starrainnotes.review.handler;

import com.starrainnotes.review.api.ReviewTargetHandler;
import com.starrainnotes.review.api.ReviewTargetRef;
import com.starrainnotes.review.api.ReviewTargetView;

/*
 * 业务模块注册回调的 SPI 注册表。
 *
 * Review 只依赖这个接口，不依赖任何具体业务模块的 Handler 类型；
 * Spring 负责把容器里所有 ReviewTargetHandler 注进来，注册表按
 * targetModule + targetType (+ reviewType) 建索引。
 *
 * 这样 Tutorial / Blog 将来只要实现 ReviewTargetHandler 并注册成 Bean 就能接入，
 * Review 侧一行都不用改，也不会产生 Maven 循环依赖。
 */
public interface ReviewTargetHandlerRegistry {

    /*
     * 查找能处理该目标的 Handler。
     * 没有任何 Handler 接单时抛 REVIEW_TARGET_NOT_SUPPORTED：
     * 提交了一个谁都不认识的 reviewType 属于接入错误，不能静默成功。
     */
    ReviewTargetHandler require(String targetModule, String targetType, String reviewType);

    /*
     * 是否存在接单的 Handler。用于提交前的快速判断，
     * 与 require 的差别是「问句」而不是「断言」。
     */
    boolean supports(String targetModule, String targetType, String reviewType);

    // 供诊断与测试使用：按 revisionRef 取视图，无 Handler 或视图不可读时返回 null
    ReviewTargetView loadView(ReviewTargetRef target, String reviewType);
}
