package com.starrainnotes.english.vocabulary.learning;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VocabularyLearningConfig {
    @Bean("vocabularyLearningClock")
    public Clock vocabularyLearningClock() { return Clock.systemUTC(); }
}
