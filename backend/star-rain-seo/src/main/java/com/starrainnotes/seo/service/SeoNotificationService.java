package com.starrainnotes.seo.service;

public interface SeoNotificationService {
    void enqueue(String routePath, String changeType);

    void processPending();
}
