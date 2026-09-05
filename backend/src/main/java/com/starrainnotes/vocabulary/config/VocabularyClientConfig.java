package com.starrainnotes.vocabulary.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Provides the shared {@link HttpClient} used by the vocabulary pronunciation
 * proxy. A bean (rather than a field construction) lets tests replace it with
 * a mock and keeps the client's connect timeout configurable in one place.
 */
@Configuration
public class VocabularyClientConfig {

    @Bean
    public HttpClient vocabularyHttpClient() {
        return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }
}
