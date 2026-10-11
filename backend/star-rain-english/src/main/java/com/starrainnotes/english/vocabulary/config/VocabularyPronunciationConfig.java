package com.starrainnotes.english.vocabulary.config;

import com.starrainnotes.english.vocabulary.properties.VocabularyPronunciationProperties;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
 * 单词发音代理的装配。
 *
 * HttpClient 交给容器（而不是在服务里 new）是为了让测试可以替换成 mock，
 * 也让连接超时只有一个配置来源。整个 V2 后端只有这一个 HttpClient Bean，
 * 因此按类型注入不会产生歧义。
 */
@Configuration
@EnableConfigurationProperties(VocabularyPronunciationProperties.class)
public class VocabularyPronunciationConfig {

    @Bean
    public HttpClient vocabularyPronunciationHttpClient(VocabularyPronunciationProperties properties) {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(1, properties.getConnectTimeoutSeconds())))
                .build();
    }
}
