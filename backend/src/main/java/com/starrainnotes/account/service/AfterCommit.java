package com.starrainnotes.account.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Runs a side effect after the current transaction commits. A rollback skips it. */
final class AfterCommit {
    private static final Logger log = LoggerFactory.getLogger(AfterCommit.class);

    private AfterCommit() {
    }

    static void run(String action, Runnable task) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            safeRun(action, task);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                safeRun(action, task);
            }
        });
    }

    private static void safeRun(String action, Runnable task) {
        try {
            task.run();
        } catch (RuntimeException ex) {
            log.warn("{} failed after the database change was committed", action, ex);
        }
    }
}
