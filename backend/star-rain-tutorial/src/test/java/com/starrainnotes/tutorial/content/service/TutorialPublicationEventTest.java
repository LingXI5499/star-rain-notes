/*
 * TutorialPublicationEventTest：教程与学习模块的测试文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：TutorialEventPublisher、TutorialPublicationChangedEvent。
 * 阅读入口：publicVersionInvalidationRunsOnlyAfterCommit（公开版本InvalidationRunsOnly之后Commit处理入口）。
 */

package com.starrainnotes.tutorial.content.service;
import static org.mockito.Mockito.*;
import com.starrainnotes.tutorial.content.event.TutorialEventPublisher;
import com.starrainnotes.tutorial.api.event.TutorialPublicationChangedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronizationManager;
/**
 * 教程发布事件测试，属于教程与学习模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class TutorialPublicationEventTest {
    /**
     * 公开版本InvalidationRunsOnly之后Commit处理入口。
     * 关键处理步骤调用 <code>TransactionSynchronizationManager.initSynchronization</code>、<code>events.afterCommit</code>、<code>TransactionSynchronizationManager.getSynchronizations</code>、<code>sync.afterCommit</code>。
     */
    @Test void publicVersionInvalidationRunsOnlyAfterCommit() {
        var publisher=mock(ApplicationEventPublisher.class);var events=new TutorialEventPublisher(publisher);
        var event=new TutorialPublicationChangedEvent(1L,"java","Java","PUBLISHED",null);
        TransactionSynchronizationManager.initSynchronization();
        try {
            events.afterCommit(event);verifyNoInteractions(publisher);
            TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());
            verify(publisher).publishEvent(event);
        } finally {TransactionSynchronizationManager.clearSynchronization();}
        reset(publisher);TransactionSynchronizationManager.initSynchronization();
        try {
            events.afterCommit(event);
            TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCompletion(1));
            verifyNoInteractions(publisher);
        } finally {TransactionSynchronizationManager.clearSynchronization();}
    }
}
