package com.starrainnotes.seo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class SeoNotificationService {
    private static final Logger log = LoggerFactory.getLogger(SeoNotificationService.class);
    private static final int MAX_ATTEMPTS = 2;
    private final SeoProperties properties;
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    public SeoNotificationService(SeoProperties properties, ObjectMapper json) {
        this.properties = properties;
        this.json = json;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void changed(SeoContentChangedEvent event) {
        if (properties.indexNowEnabled() && !SeoProperties.blank(properties.indexNowKey())) submitIndexNow(event.absoluteUrl());
        if (properties.baiduEnabled() && !SeoProperties.blank(properties.baiduToken())) submitBaidu(event.absoluteUrl());
    }

    private void submitIndexNow(String url) {
        try {
            String body = json.writeValueAsString(Map.of("host", URI.create(properties.siteOrigin()).getHost(), "key", properties.indexNowKey(), "keyLocation", properties.siteOrigin() + "/indexnow-key.txt", "urlList", List.of(url)));
            send(HttpRequest.newBuilder(URI.create("https://api.indexnow.org/indexnow")).timeout(Duration.ofSeconds(5)).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build(), "IndexNow");
        } catch (Exception e) { log.warn("IndexNow notification failed for {}: {}", url, e.getMessage()); }
    }

    private void submitBaidu(String url) {
        try {
            String endpoint = "https://data.zz.baidu.com/urls?site=" + URLEncoder.encode(properties.baiduSite(), StandardCharsets.UTF_8) + "&token=" + URLEncoder.encode(properties.baiduToken(), StandardCharsets.UTF_8);
            send(HttpRequest.newBuilder(URI.create(endpoint)).timeout(Duration.ofSeconds(5)).header("Content-Type", "text/plain").POST(HttpRequest.BodyPublishers.ofString(url)).build(), "Baidu");
        } catch (Exception e) { log.warn("Baidu notification failed for {}: {}", url, e.getMessage()); }
    }

    private void send(HttpRequest request, String provider) throws Exception {
        Exception lastFailure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                HttpResponse<Void> response = http.send(request, HttpResponse.BodyHandlers.discarding());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    log.info("{} accepted SEO URL notification", provider);
                    return;
                }
                lastFailure = new IllegalStateException(provider + " returned HTTP " + response.statusCode());
                // Authentication and request errors are not transient and must not be retried.
                if (response.statusCode() >= 400 && response.statusCode() < 500) break;
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw interrupted;
            } catch (Exception failure) {
                lastFailure = failure;
            }
        }
        throw lastFailure == null ? new IllegalStateException(provider + " notification failed") : lastFailure;
    }
}
