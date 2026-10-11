/*
 * ReviewTargetHandlerRegistryImpl：审核模块的模块支持文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 * 主要协作类型：ReviewTargetHandler、ReviewTargetRef、ReviewTargetView、ReviewDecisionConflictException、ReviewTargetNotSupportedException、ReviewTargetHandlerRegistry。
 * 阅读入口：require（检查所需审核目标处理器Registry实现）；supports（判断是否支持审核目标处理器Registry实现）；loadView（加载View）。
 */

package com.starrainnotes.review.handler.impl;

import com.starrainnotes.review.api.ReviewTargetHandler;
import com.starrainnotes.review.api.ReviewTargetRef;
import com.starrainnotes.review.api.ReviewTargetView;
import com.starrainnotes.review.exception.ReviewDecisionConflictException;
import com.starrainnotes.review.exception.ReviewTargetNotSupportedException;
import com.starrainnotes.review.handler.ReviewTargetHandlerRegistry;
import java.util.List;
import org.springframework.stereotype.Component;

/*
 * SPI 注册表实现。
 *
 * 路由规则只有一条：按 Handler 自己声明的 targetModule + targetType + supports(reviewType) 精确匹配。
 *
 * 刻意不做「唯一覆盖该目标类型就兜底接单」的宽松匹配：那会让一个只认领 tutorial.publish
 * 的 Handler 悄悄接走 tutorial.archive，把审核回调发给错误的业务分支。
 * 宁可返回 REVIEW_TARGET_NOT_SUPPORTED 让接入方显式声明 reviewType，也不要静默猜。
 * 接入方大多数只有一种审核时，ReviewTargetHandler.supports 的默认实现（只比 module + type）
 * 已经完全够用，所以精确匹配并不会给接入方增加负担。
 *
 * 命中多个 Handler 同样视为接入错误：同一个 (module, type, reviewType) 只能有一个 Handler 负责。
 *
 * Handler 在构造时一次性拷贝，运行期不再变化；业务模块在启动时注册 Bean，
 * 因此这里不存在并发可见性问题，拷贝同时也防止外部 list 被改动影响路由结果。
 */
/**
 * 审核目标处理器Registry实现，属于审核模块。
 * 提供包内其它组件使用的支持能力，主要协作类型及入口方法见本文件说明。
 */
@Component
public class ReviewTargetHandlerRegistryImpl implements ReviewTargetHandlerRegistry {

    /**
     * handlers。
     */
    private final List<ReviewTargetHandler> handlers;

    /**
     * 构造ReviewTargetHandlerRegistryImpl，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>List.copyOf</code>。
     *
     * @param handlers handlers；类型为 <code>List&lt;ReviewTargetHandler&gt;</code>
     */
    public ReviewTargetHandlerRegistryImpl(List<ReviewTargetHandler> handlers) {
        // 只做一次拷贝，防止外部 list 后续被改动影响路由结果
        this.handlers = List.copyOf(handlers);
    }

    /**
     * 检查所需审核目标处理器Registry实现。
     * 关键处理步骤调用 <code>find</code>。
     * 实现中的显式失败类型包括 <code>ReviewTargetNotSupportedException</code>；具体触发条件见对应分支。
     *
     * @param targetModule 目标对象所属模块；类型为 <code>String</code>
     * @param targetType 目标对象的业务类型；类型为 <code>String</code>
     * @param reviewType 审核类型；类型为 <code>String</code>
     * @return 类型为 <code>ReviewTargetHandler</code> 的处理结果
     */
    @Override
    public ReviewTargetHandler require(String targetModule, String targetType, String reviewType) {
        ReviewTargetHandler handler = find(targetModule, targetType, reviewType);
        if (handler == null) {
            throw new ReviewTargetNotSupportedException();
        }
        return handler;
    }

    /**
     * 判断是否支持审核目标处理器Registry实现。
     * 关键处理步骤调用 <code>find</code>。
     *
     * @param targetModule 目标对象所属模块；类型为 <code>String</code>
     * @param targetType 目标对象的业务类型；类型为 <code>String</code>
     * @param reviewType 审核类型；类型为 <code>String</code>
     * @return 本方法条件是否成立的判断结果
     */
    @Override
    public boolean supports(String targetModule, String targetType, String reviewType) {
        return find(targetModule, targetType, reviewType) != null;
    }

    /**
     * 加载View。
     * 关键处理步骤调用 <code>find</code>、<code>target.getTargetModule</code>、<code>target.getTargetType</code>、<code>handler.loadReviewView</code>。
     *
     * @param target 目标；类型为 <code>ReviewTargetRef</code>
     * @param reviewType 审核类型；类型为 <code>String</code>
     * @return 类型为 <code>ReviewTargetView</code> 的处理结果
     */
    @Override
    public ReviewTargetView loadView(ReviewTargetRef target, String reviewType) {
        if (target == null) {
            return null;
        }
        ReviewTargetHandler handler = find(target.getTargetModule(), target.getTargetType(), reviewType);
        return handler == null ? null : handler.loadReviewView(target);
    }

    /**
     * 查找审核目标处理器Registry实现。
     * 关键处理步骤调用 <code>handler.supports</code>。
     * 实现中的显式失败类型包括 <code>ReviewDecisionConflictException</code>；具体触发条件见对应分支。
     *
     * @param targetModule 目标对象所属模块；类型为 <code>String</code>
     * @param targetType 目标对象的业务类型；类型为 <code>String</code>
     * @param reviewType 审核类型；类型为 <code>String</code>
     * @return 类型为 <code>ReviewTargetHandler</code> 的处理结果
     */
    private ReviewTargetHandler find(String targetModule, String targetType, String reviewType) {
        if (targetModule == null || targetType == null) {
            return null;
        }
        ReviewTargetHandler matched = null;
        for (ReviewTargetHandler handler : handlers) {
            if (handler.supports(targetModule, targetType, reviewType)) {
                if (matched != null) {
                    // 两个 Handler 抢同一个 reviewType：属于接入配置冲突，必须炸出来
                    throw new ReviewDecisionConflictException();
                }
                matched = handler;
            }
        }
        return matched;
    }
}
