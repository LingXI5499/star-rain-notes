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

class EnglishSearchIndexTest {
    private final EnglishSearchSourceApi source = mock(EnglishSearchSourceApi.class);
    private final SearchIndexApi index = mock(SearchIndexApi.class);
    private final SearchDocumentMapper mapper = mock(SearchDocumentMapper.class);
    private final SearchRebuildServiceImpl rebuild = new SearchRebuildServiceImpl(null, null, null, null,
        index, mapper, new SearchTextExtractor(), source);

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
