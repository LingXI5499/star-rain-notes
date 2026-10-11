package com.starrainnotes.profile.event;

import com.starrainnotes.profile.api.event.ProfileChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class ProfileEventPublisher {
    private final ApplicationEventPublisher publisher;

    public void afterCommit(Long profileId) {
        ProfileChangedEvent event = new ProfileChangedEvent(profileId);
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
