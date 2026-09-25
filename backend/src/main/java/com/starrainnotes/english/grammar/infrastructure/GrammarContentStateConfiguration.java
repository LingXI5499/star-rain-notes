package com.starrainnotes.english.grammar.infrastructure;

import com.starrainnotes.english.shared.events.EnglishContentKind;
import com.starrainnotes.english.shared.events.EnglishContentStateSource;
import com.starrainnotes.english.shared.events.JdbcContentStateSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class GrammarContentStateConfiguration {
    @Bean
    EnglishContentStateSource grammarLessonContentState(JdbcTemplate jdbc) {
        return new JdbcContentStateSource(EnglishContentKind.GRAMMAR_LESSON, jdbc, "english_grammar_lesson");
    }
}
