package com.starrainnotes.english.shared.bundle.infrastructure;

import com.starrainnotes.english.shared.events.EnglishContentKind;
import com.starrainnotes.english.shared.events.EnglishContentStateSource;
import com.starrainnotes.english.shared.events.JdbcContentStateSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class BundleContentStateConfiguration {
    @Bean
    EnglishContentStateSource bundleContentState(JdbcTemplate jdbc) {
        return new JdbcContentStateSource(EnglishContentKind.BUNDLE, jdbc, "english_learning_bundle");
    }
}
