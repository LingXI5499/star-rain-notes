/*
 * VocabularyPronunciationConfig：英语学习模块的装配配置文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 向 Spring 容器装配本模块使用的组件或注册规则，方法上的 Bean 与配置注解决定装配方式。
 * 主要协作类型：VocabularyPronunciationProperties。
 * 阅读入口：vocabularyPronunciationHttpClient（词汇PronunciationHTTP客户端处理入口）。
 */

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
/**
 * 词汇Pronunciation配置，属于英语学习模块。
 * 向 Spring 容器装配本模块使用的组件或注册规则，方法上的 Bean 与配置注解决定装配方式。
 */
@Configuration
@EnableConfigurationProperties(VocabularyPronunciationProperties.class)
public class VocabularyPronunciationConfig {

    /**
     * 词汇PronunciationHTTP客户端处理入口。
     * 关键处理步骤调用 <code>HttpClient.newBuilder</code>、<code>Duration.ofSeconds</code>、<code>Math.max</code>、<code>properties.getConnectTimeoutSeconds</code>。
     *
     * @param properties 本模块的外部配置属性；类型为 <code>VocabularyPronunciationProperties</code>
     * @return 类型为 <code>HttpClient</code> 的处理结果
     */
    @Bean
    public HttpClient vocabularyPronunciationHttpClient(VocabularyPronunciationProperties properties) {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(1, properties.getConnectTimeoutSeconds())))
                .build();
    }
}
