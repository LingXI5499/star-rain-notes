package com.starrainnotes.tutorial.content.service;
import static org.mockito.Mockito.*;
import com.starrainnotes.tutorial.content.event.TutorialEventPublisher;
import com.starrainnotes.tutorial.api.event.TutorialPublicationChangedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronizationManager;
class TutorialPublicationEventTest {
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
