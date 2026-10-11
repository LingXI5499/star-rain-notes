package com.starrainnotes.portfolio.event;

import com.starrainnotes.portfolio.api.event.WorkPublicationChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class WorkEventPublisher {
    private final ApplicationEventPublisher publisher;

    public void afterCommit(WorkPublicationChangedEvent event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publisher.publishEvent(event);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publisher.publishEvent(event);
            }
        });
    }
}
