package com.starrainnotes.analytics.classifier;

public class ReferrerClassification {
    private final String category;
    private final String host;

    public ReferrerClassification(String category, String host) {
        this.category = category;
        this.host = host;
    }

    public String getCategory() { return category; }
    public String getHost() { return host; }
}
