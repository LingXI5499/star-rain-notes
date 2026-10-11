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
class ReviewDecisionCallbackDispatcherTest {

    private final ReviewDecisionCallbackDispatcher dispatcher = new ReviewDecisionCallbackDispatcher();

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("没有活动事务时立即执行：回调不会被静默丢弃")
    void runsImmediatelyWithoutTransaction() {
        AtomicInteger counter = new AtomicInteger();

        dispatcher.dispatch(counter::incrementAndGet);

        assertThat(counter.get()).isEqualTo(1);
    }

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

    @Test
    @DisplayName("null 回调被安静忽略，不抛异常")
    void ignoresNullCallback() {
        dispatcher.dispatch(null);

        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    }
}
