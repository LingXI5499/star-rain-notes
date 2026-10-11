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
@Component
public class ReviewTargetHandlerRegistryImpl implements ReviewTargetHandlerRegistry {

    private final List<ReviewTargetHandler> handlers;

    public ReviewTargetHandlerRegistryImpl(List<ReviewTargetHandler> handlers) {
        // 只做一次拷贝，防止外部 list 后续被改动影响路由结果
        this.handlers = List.copyOf(handlers);
    }

    @Override
    public ReviewTargetHandler require(String targetModule, String targetType, String reviewType) {
        ReviewTargetHandler handler = find(targetModule, targetType, reviewType);
        if (handler == null) {
            throw new ReviewTargetNotSupportedException();
        }
        return handler;
    }

    @Override
    public boolean supports(String targetModule, String targetType, String reviewType) {
        return find(targetModule, targetType, reviewType) != null;
    }

    @Override
    public ReviewTargetView loadView(ReviewTargetRef target, String reviewType) {
        if (target == null) {
            return null;
        }
        ReviewTargetHandler handler = find(target.getTargetModule(), target.getTargetType(), reviewType);
        return handler == null ? null : handler.loadReviewView(target);
    }

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
