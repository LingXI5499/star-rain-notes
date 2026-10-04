package com.starrainnotes.seo.notify;

public interface SearchEngineNotifier {
    String providerCode();
    boolean enabled();
    NotificationResult notify(SeoChange change);
}
