/*
 * EnglishSearchIndexTest：搜索模块的测试文件。
 * 业务范围：搜索文档投影、索引重建和关键词查询。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：EnglishSearchSourceApi、EnglishSearchTypes、EnglishSearchDocument、EnglishSearchContentChangedEvent、SearchIndexApi、SearchableDocument、SearchEventConsumer。
 * 阅读入口：syncUpdatesPublicBodiesAndRemovesWithdrawnOrPrivateDocuments（同步Updates公开Bodies及Removes已撤回或PrivateDocuments）；rebuildUsesIdCursorAndRemovesStaleKeysOnlyForTheRequestedType（重建Uses标识Cursor及RemovesStaleKeysOnly面向TheRequested类型）；existingInstallationsBackfillEveryEnglishTypeAtStartup（existingInstallationsBackfillEvery英语类型时间Startup处理入口）；indexAcceptsEnglishTypes（索引Accepts英语Types处理入口）。
 */

package com.starrainnotes.search;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.starrainnotes.english.api.EnglishSearchSourceApi;
import com.starrainnotes.english.api.EnglishSearchTypes;
import com.starrainnotes.english.api.dto.EnglishSearchDocument;
import com.starrainnotes.english.api.event.EnglishSearchContentChangedEvent;
import com.starrainnotes.search.api.SearchIndexApi;
import com.starrainnotes.search.api.dto.SearchableDocument;
import com.starrainnotes.search.event.SearchEventConsumer;
import com.starrainnotes.search.mapper.SearchDocumentMapper;
import com.starrainnotes.search.service.*;
import com.starrainnotes.search.service.impl.SearchRebuildServiceImpl;
import com.starrainnotes.search.utils.SearchTextExtractor;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.event.TransactionalEventListenerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * 英语搜索索引测试，属于搜索模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class EnglishSearchIndexTest {
    /**
     * 来源。
     * 协作类型：<code>EnglishSearchSourceApi</code>。
     */
    private final EnglishSearchSourceApi source = mock(EnglishSearchSourceApi.class);
    /**
     * 索引。
     * 协作类型：<code>SearchIndexApi</code>。
     */
    private final SearchIndexApi index = mock(SearchIndexApi.class);
    /**
     * 数据映射。
     * 协作类型：<code>SearchDocumentMapper</code>。
     */
    private final SearchDocumentMapper mapper = mock(SearchDocumentMapper.class);
    /**
     * rebuild。
     */
    private final SearchRebuildServiceImpl rebuild = new SearchRebuildServiceImpl(null, null, null, null,
        index, mapper, new SearchTextExtractor(), source);

    /**
     * 同步Updates公开Bodies及Removes已撤回或PrivateDocuments。
     * 主要协作调用：<code>source.findPublished</code>、<code>rebuild.syncEnglish</code>。
     */
    @Test
    void syncUpdatesPublicBodiesAndRemovesWithdrawnOrPrivateDocuments() {
        var document = document(12L);
        when(source.findPublished("ENGLISH_READING", 12L)).thenReturn(document);
        rebuild.syncEnglish("ENGLISH_READING", 12L);
        var captured = ArgumentCaptor.forClass(SearchableDocument.class);
        verify(index).upsert(captured.capture());
        assertEquals("ENGLISH_READING:12", captured.getValue().getDocumentKey());
        assertEquals("English body 中文正文", captured.getValue().getSearchableText());
        assertEquals("/english/reading/reading-12", captured.getValue().getRoutePath());
        when(source.findPublished("ENGLISH_READING", 12L)).thenReturn(null);
        rebuild.syncEnglish("ENGLISH_READING", 12L);
        verify(index).removeByContent("ENGLISH_READING", 12L);
    }

    /**
     * 重建Uses标识Cursor及RemovesStaleKeysOnly面向TheRequested类型。
     * 主要协作调用：<code>source.page</code>、<code>mapper.activeKeys</code>、<code>rebuild.rebuildType</code>。
     */
    @Test
    void rebuildUsesIdCursorAndRemovesStaleKeysOnlyForTheRequestedType() {
        var first = java.util.stream.LongStream.rangeClosed(1, 100).mapToObj(this::document).toList();
        when(source.page("ENGLISH_READING", null, 100)).thenReturn(first);
        when(source.page("ENGLISH_READING", 100L, 100)).thenReturn(List.of(document(101L)));
        when(mapper.activeKeys("ENGLISH_READING"))
            .thenReturn(List.of("ENGLISH_READING:1", "ENGLISH_READING:102"));
        rebuild.rebuildType("ENGLISH_READING");
        verify(index, times(101)).upsert(any());
        verify(index).remove("ENGLISH_READING:102");
        verify(index, never()).remove("ENGLISH_READING:1");
        verify(source).page("ENGLISH_READING", 100L, 100);
        verify(mapper, never()).activeKeys("BLOG");
    }

    /**
     * existingInstallationsBackfillEvery英语类型时间Startup处理入口。
     * 主要协作调用：<code>mapper.activeCount</code>、<code>source.page</code>、<code>rebuild.rebuildType</code>。
     */
    @Test
    void existingInstallationsBackfillEveryEnglishTypeAtStartup() {
        var service = mock(SearchRebuildService.class);
        when(mapper.activeCount()).thenReturn(200L);
        new SearchStartupRebuild(mapper, service).seedEmptyIndex();
        verify(service).rebuildType("ENGLISH");
        when(source.page(anyString(), isNull(), eq(100))).thenReturn(List.of());
        rebuild.rebuildType("ENGLISH");
        for (String type : EnglishSearchTypes.ALL) verify(source).page(type, null, 100);
    }

    /**
     * 索引Accepts英语Types处理入口。
     * 关键处理步骤调用 <code>document.setContentType</code>、<code>document.setContentId</code>、<code>document.setDocumentKey</code>、<code>document.setTitle</code>。
     */
    @Test
    void indexAcceptsEnglishTypes() {
        var service = new SearchIndexService(mapper);
        var document = new SearchableDocument();
        document.setContentType("ENGLISH_WRITING");
        document.setContentId(12L);
        document.setDocumentKey("ENGLISH_WRITING:12");
        document.setTitle("Original article");
        document.setRoutePath("/english/writing/author/writing-12");
        service.upsert(document);
        verify(mapper).upsert(document);
    }

    /**
     * 英语事件集合Wait面向Commit及AreDiscarded触发回滚处理入口。
     * 关键处理步骤调用 <code>context.registerBean</code>、<code>context.refresh</code>、<code>TransactionSynchronizationManager.setActualTransactionActive</code>、<code>TransactionSynchronizationManager.initSynchronization</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test
    void englishEventsWaitForCommitAndAreDiscardedOnRollback() throws Exception {
        var service = mock(SearchRebuildService.class);
        var consumer = new SearchEventConsumer(service, null, null, null);
        try (var context = new AnnotationConfigApplicationContext()) {
            context.registerBean("consumer", SearchEventConsumer.class, () -> consumer);
            context.registerBean(TransactionalEventListenerFactory.class);
            context.refresh();
            var event = new EnglishSearchContentChangedEvent("ENGLISH_WRITING", 12L);
            TransactionSynchronizationManager.setActualTransactionActive(true);
            TransactionSynchronizationManager.initSynchronization();
            try {
                context.publishEvent(event);
                verifyNoInteractions(service);
                for (var sync : TransactionSynchronizationManager.getSynchronizations()) {
                    sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
                }
                verifyNoInteractions(service);
            } finally { TransactionSynchronizationManager.clear(); }
            TransactionSynchronizationManager.setActualTransactionActive(true);
            TransactionSynchronizationManager.initSynchronization();
            try {
                context.publishEvent(event);
                verifyNoInteractions(service);
                for (var sync : TransactionSynchronizationManager.getSynchronizations()) {
                    sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
                }
                verify(service).syncEnglish("ENGLISH_WRITING", 12L);
            } finally { TransactionSynchronizationManager.clear(); }
            consumer.onEnglishChanged(new EnglishSearchContentChangedEvent("ENGLISH_GRAMMAR_LESSON", null));
            verify(service).rebuildType("ENGLISH_GRAMMAR_LESSON");
        }
    }

    /**
     * 文档处理入口。
     * 关键处理步骤调用 <code>document.setContentType</code>、<code>document.setId</code>、<code>document.setTitle</code>、<code>document.setSearchableText</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     * @return 类型为 <code>EnglishSearchDocument</code> 的处理结果
     */
    private EnglishSearchDocument document(long id) {
        var document = new EnglishSearchDocument();
        document.setContentType("ENGLISH_READING");
        document.setId(id);
        document.setTitle("Article " + id);
        document.setSearchableText("English **body** 中文正文");
        document.setRoutePath("/english/reading/reading-" + id);
        return document;
    }
}
