/*
 * ReviewDecisionCallbackDispatcherTest：审核模块的测试文件。
 * 业务范围：审批请求、审核步骤、目标锁和审批后的业务应用。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 阅读入口：clearSynchronization（清理Synchronization）；runsImmediatelyWithoutTransaction（\u6ca1\u6709\u6d3b\u52a8\u4e8b\u52a1\u65f6\u7acb\u5373\u6267\u884c\uff1a\u56de\u8c03\u4e0d\u4f1a\u88ab\u9759\u9ed8\u4e22\u5f03）；defersUntilAfterCommit（\u6709\u6d3b\u52a8\u4e8b\u52a1\u65f6\u6ce8\u518c\u4e3a afterCommit\uff1a\u63d0\u4ea4\u524d\u4e0d\u6267\u884c\uff0c\u63d0\u4ea4\u540e\u624d\u6267\u884c）；doesNotRunOnRollback（\u4e8b\u52a1\u56de\u6eda\u65f6\u4e0d\u6267\u884c\u56de\u8c03\uff1a\u5ba1\u6838\u51b3\u5b9a\u6ca1\u751f\u6548\u5c31\u4e0d\u8be5\u63a8\u8fdb\u4e1a\u52a1\u72b6\u6001）。
 */

package com.starrainnotes.review.handler;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/*
 * 审核结论回调调度器测试。
 *
 * 这是「审核决定先提交、业务模块随后推进状态」这条架构约束的落点，
 * 因此要精确验证 afterCommit 语义：注册之后不会立刻执行，
 * 只有事务提交（或没有事务时的立即执行）才真正调用回调。
 */
/**
 * 审核决定CallbackDispatcher测试，属于审核模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class ReviewDecisionCallbackDispatcherTest {

    /**
     * dispatcher。
     */
    private final ReviewDecisionCallbackDispatcher dispatcher = new ReviewDecisionCallbackDispatcher();

    /**
     * 清理Synchronization。
     * 关键处理步骤调用 <code>TransactionSynchronizationManager.isSynchronizationActive</code>、<code>TransactionSynchronizationManager.clearSynchronization</code>。
     */
    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    /**
     * \u6ca1\u6709\u6d3b\u52a8\u4e8b\u52a1\u65f6\u7acb\u5373\u6267\u884c\uff1a\u56de\u8c03\u4e0d\u4f1a\u88ab\u9759\u9ed8\u4e22\u5f03。
     * 主要协作调用：<code>dispatcher.dispatch</code>。
     */
    @Test
    @DisplayName("没有活动事务时立即执行：回调不会被静默丢弃")
    void runsImmediatelyWithoutTransaction() {
        AtomicInteger counter = new AtomicInteger();

        dispatcher.dispatch(counter::incrementAndGet);

        assertThat(counter.get()).isEqualTo(1);
    }

    /**
     * \u6709\u6d3b\u52a8\u4e8b\u52a1\u65f6\u6ce8\u518c\u4e3a afterCommit\uff1a\u63d0\u4ea4\u524d\u4e0d\u6267\u884c\uff0c\u63d0\u4ea4\u540e\u624d\u6267\u884c。
     * 主要协作调用：<code>dispatcher.dispatch</code>。
     */
    @Test
    @DisplayName("有活动事务时注册为 afterCommit：提交前不执行，提交后才执行")
    void defersUntilAfterCommit() {
        AtomicInteger counter = new AtomicInteger();
        TransactionSynchronizationManager.initSynchronization();

        dispatcher.dispatch(counter::incrementAndGet);

        // 关键：事务还没提交，业务模块绝不能先于审核决定看到回调
        assertThat(counter.get()).isZero();

        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();
        assertThat(synchronizations).hasSize(1);
        synchronizations.forEach(TransactionSynchronization::afterCommit);

        assertThat(counter.get()).isEqualTo(1);
    }

    /**
     * \u4e8b\u52a1\u56de\u6eda\u65f6\u4e0d\u6267\u884c\u56de\u8c03\uff1a\u5ba1\u6838\u51b3\u5b9a\u6ca1\u751f\u6548\u5c31\u4e0d\u8be5\u63a8\u8fdb\u4e1a\u52a1\u72b6\u6001。
     * 主要协作调用：<code>dispatcher.dispatch</code>。
     */
    @Test
    @DisplayName("事务回滚时不执行回调：审核决定没生效就不该推进业务状态")
    void doesNotRunOnRollback() {
        AtomicInteger counter = new AtomicInteger();
        TransactionSynchronizationManager.initSynchronization();

        dispatcher.dispatch(counter::incrementAndGet);
        // 模拟回滚：只触发 afterCompletion，不触发 afterCommit
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization -> synchronization.afterCompletion(
                        org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK));

        assertThat(counter.get()).isZero();
    }

    /**
     * \u591a\u4e2a\u56de\u8c03\u6309\u6ce8\u518c\u987a\u5e8f\u6267\u884c\uff1a\u5148\u5199\u5386\u53f2\u7684\u56de\u8c03\u4e0d\u4f1a\u88ab\u540e\u6ce8\u518c\u7684\u6324\u6389。
     * 主要协作调用：<code>dispatcher.dispatch</code>。
     */
    @Test
    @DisplayName("多个回调按注册顺序执行：先写历史的回调不会被后注册的挤掉")
    void runsMultipleCallbacksInRegistrationOrder() {
        StringBuilder order = new StringBuilder();
        TransactionSynchronizationManager.initSynchronization();

        dispatcher.dispatch(() -> order.append('A'));
        dispatcher.dispatch(() -> order.append('B'));

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);

        assertThat(order.toString()).isEqualTo("AB");
    }

    /**
     * null \u56de\u8c03\u88ab\u5b89\u9759\u5ffd\u7565\uff0c\u4e0d\u629b\u5f02\u5e38。
     * 主要协作调用：<code>dispatcher.dispatch</code>。
     */
    @Test
    @DisplayName("null 回调被安静忽略，不抛异常")
    void ignoresNullCallback() {
        dispatcher.dispatch(null);

        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    }
}
