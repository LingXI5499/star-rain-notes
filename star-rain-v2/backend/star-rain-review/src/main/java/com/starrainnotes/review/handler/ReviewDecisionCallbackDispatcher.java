package com.starrainnotes.review.handler;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/*
 * 审核结论回调的调度器。
 *
 * 为什么一定要「事务提交之后」：
 *   审核决定（PENDING → APPROVED/REJECTED/CANCELED）是 Review 的核心事实，
 *   而目标模块推进自己的状态是派生动作。如果两者在同一事务里，目标模块的一次
 *   偶发失败（比如它自己的唯一约束冲突）会把审核决定一起回滚，
 *   导致 Reviewer 明明点了通过却什么都没发生。
 *   因此这里注册 afterCommit 回调：审核决定先落库并提交，回调随后执行，
 *   回调失败只影响目标模块自己的补偿流程，不会推翻已经生效的审核结论。
 *
 * 没有活动事务时（例如纯单元测试、或将来从非事务上下文调用）立即执行，
 * 保证行为可预期，而不是静默丢弃回调。
 */
@Component
public class ReviewDecisionCallbackDispatcher {

    public void dispatch(Runnable callback) {
        if (callback == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // 没有事务就立即执行：宁可立刻失败被调用方看到，也不要丢掉业务回调
            callback.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                callback.run();
            }
        });
    }
}
