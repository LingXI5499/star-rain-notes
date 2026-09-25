package com.starrainnotes.english.reading.infrastructure;

import com.starrainnotes.english.shared.events.EnglishContentKind;
import com.starrainnotes.english.shared.events.EnglishContentStateSource;
import com.starrainnotes.english.shared.events.JdbcContentStateSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class ReadingContentStateConfiguration {
    @Bean
    EnglishContentStateSource readingContentState(JdbcTemplate jdbc) {
        return new JdbcContentStateSource(EnglishContentKind.READING, jdbc, "english_reading_article");
    }
}
