/*
 * TutorialEventPublisher：教程与学习模块的事件协作文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 定义或消费业务事件，用于把内容变化同步给其它模块；事件携带字段和处理方法在下文说明。
 * 主要协作类型：TutorialPublicationChangedEvent。
 * 阅读入口：afterCommit（之后Commit处理入口）；afterCommit（之后Commit处理入口）。
 */

package com.starrainnotes.tutorial.content.event;

import com.starrainnotes.tutorial.api.event.TutorialPublicationChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 教程事件发布器，属于教程与学习模块。
 * 定义或消费业务事件，用于把内容变化同步给其它模块；事件携带字段和处理方法在下文说明。
 */
@Component
@RequiredArgsConstructor
public class TutorialEventPublisher {
    /**
     * 发布器。
     */
    private final ApplicationEventPublisher publisher;

    /**
     * 之后Commit处理入口。
     * 主要协作调用：<code>publisher.publishEvent</code>。
     *
     * @param event 事件；类型为 <code>TutorialPublicationChangedEvent</code>
     */
    public void afterCommit(TutorialPublicationChangedEvent event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publisher.publishEvent(event);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization()
    /**
     * 当前调用使用的匿名协作实现，属于教程与学习模块。
     * 定义或消费业务事件，用于把内容变化同步给其它模块；事件携带字段和处理方法在下文说明。
     */
    {
            /**
             * 之后Commit处理入口。
             * 关键处理步骤调用 <code>publisher.publishEvent</code>。
             */
            @Override
            public void afterCommit() {
                publisher.publishEvent(event);
            }
        });
    }
}
