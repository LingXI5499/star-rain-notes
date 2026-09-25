package com.starrainnotes.english.writing.infrastructure;

import com.starrainnotes.english.shared.events.EnglishContentKind;
import com.starrainnotes.english.shared.events.EnglishContentStateSource;
import com.starrainnotes.english.shared.events.JdbcContentStateSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class WritingContentStateConfiguration {
    @Bean
    EnglishContentStateSource writingPromptContentState(JdbcTemplate jdbc) {
        return new JdbcContentStateSource(EnglishContentKind.WRITING_PROMPT, jdbc, "english_writing_prompt");
    }

    @Bean
    EnglishContentStateSource writingResourceContentState(JdbcTemplate jdbc) {
        return new JdbcContentStateSource(EnglishContentKind.WRITING_RESOURCE, jdbc, "english_writing_resource");
    }
}
