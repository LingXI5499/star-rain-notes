package com.starrainnotes.english.listening.infrastructure;

import com.starrainnotes.english.shared.events.EnglishContentKind;
import com.starrainnotes.english.shared.events.EnglishContentStateSource;
import com.starrainnotes.english.shared.events.JdbcContentStateSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class ListeningContentStateConfiguration {
    @Bean
    EnglishContentStateSource listeningContentState(JdbcTemplate jdbc) {
        return new JdbcContentStateSource(EnglishContentKind.LISTENING, jdbc, "english_listening_item");
    }

    @Bean
    EnglishContentStateSource pronunciationContentState(JdbcTemplate jdbc) {
        return new JdbcContentStateSource(EnglishContentKind.PRONUNCIATION_RULE, jdbc,
                "english_listening_pronunciation_rule");
    }
}
