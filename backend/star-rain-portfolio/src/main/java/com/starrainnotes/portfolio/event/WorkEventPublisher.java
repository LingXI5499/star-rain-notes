/*
 * WorkEventPublisher：作品模块的事件协作文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 定义或消费业务事件，用于把内容变化同步给其它模块；事件携带字段和处理方法在下文说明。
 * 主要协作类型：WorkPublicationChangedEvent。
 * 阅读入口：afterCommit（之后Commit处理入口）；afterCommit（之后Commit处理入口）。
 */

package com.starrainnotes.portfolio.event;

import com.starrainnotes.portfolio.api.event.WorkPublicationChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 作品事件发布器，属于作品模块。
 * 定义或消费业务事件，用于把内容变化同步给其它模块；事件携带字段和处理方法在下文说明。
 */
@Component
@RequiredArgsConstructor
public class WorkEventPublisher {
    /**
     * 发布器。
     */
    private final ApplicationEventPublisher publisher;

    /**
     * 之后Commit处理入口。
     * 主要协作调用：<code>publisher.publishEvent</code>。
     *
     * @param event 事件；类型为 <code>WorkPublicationChangedEvent</code>
     */
    public void publish(WorkPublicationChangedEvent event) {
        publisher.publishEvent(event);
    }
}
