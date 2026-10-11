/*
 * ReviewTargetHandlerRegistry：审核模块的模块支持文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 * 主要协作类型：ReviewTargetHandler、ReviewTargetRef、ReviewTargetView。
 * 阅读入口：require（检查所需审核目标处理器Registry）；supports（判断是否支持审核目标处理器Registry）；loadView（加载View）。
 */

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
/**
 * 审核目标处理器Registry，属于审核模块。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 */
public interface ReviewTargetHandlerRegistry {

    /*
     * 查找能处理该目标的 Handler。
     * 没有任何 Handler 接单时抛 REVIEW_TARGET_NOT_SUPPORTED：
     * 提交了一个谁都不认识的 reviewType 属于接入错误，不能静默成功。
     */
    /**
     * 检查所需审核目标处理器Registry。
     *
     * @param targetModule 目标对象所属模块；类型为 <code>String</code>
     * @param targetType 目标对象的业务类型；类型为 <code>String</code>
     * @param reviewType 审核类型；类型为 <code>String</code>
     * @return 类型为 <code>ReviewTargetHandler</code> 的处理结果
     */
    ReviewTargetHandler require(String targetModule, String targetType, String reviewType);

    /*
     * 是否存在接单的 Handler。用于提交前的快速判断，
     * 与 require 的差别是「问句」而不是「断言」。
     */
    /**
     * 判断是否支持审核目标处理器Registry。
     *
     * @param targetModule 目标对象所属模块；类型为 <code>String</code>
     * @param targetType 目标对象的业务类型；类型为 <code>String</code>
     * @param reviewType 审核类型；类型为 <code>String</code>
     * @return 本方法条件是否成立的判断结果
     */
    boolean supports(String targetModule, String targetType, String reviewType);

    // 供诊断与测试使用：按 revisionRef 取视图，无 Handler 或视图不可读时返回 null
    /**
     * 加载View。
     *
     * @param target 目标；类型为 <code>ReviewTargetRef</code>
     * @param reviewType 审核类型；类型为 <code>String</code>
     * @return 类型为 <code>ReviewTargetView</code> 的处理结果
     */
    ReviewTargetView loadView(ReviewTargetRef target, String reviewType);
}
