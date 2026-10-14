/*
 * ProfileEventPublisher：作者资料模块的事件协作文件。
 * 业务范围：个人介绍、社交链接、经历技能和精选内容。
 * 定义或消费业务事件，用于把内容变化同步给其它模块；事件携带字段和处理方法在下文说明。
 * 主要协作类型：ProfileChangedEvent。
 * 阅读入口：afterCommit（之后Commit处理入口）；afterCommit（之后Commit处理入口）。
 */

package com.starrainnotes.profile.event;

import com.starrainnotes.profile.api.event.ProfileChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 资料事件发布器，属于作者资料模块。
 * 定义或消费业务事件，用于把内容变化同步给其它模块；事件携带字段和处理方法在下文说明。
 */
@Component
@RequiredArgsConstructor
public class ProfileEventPublisher {
    /**
     * 发布器。
     */
    private final ApplicationEventPublisher publisher;

    /**
     * 之后Commit处理入口。
     * 主要协作调用：<code>publisher.publishEvent</code>。
     *
     * @param profileId 资料标识；类型为 <code>Long</code>
     */
    public void publish(Long profileId) {
        publisher.publishEvent(new ProfileChangedEvent(profileId));
    }
}
