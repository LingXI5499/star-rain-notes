package com.starrainnotes.tutorial.learning.config;

import com.starrainnotes.tutorial.learning.properties.LearningReviewProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableConfigurationProperties(LearningReviewProperties.class)
@EnableScheduling
public class TutorialLearningConfiguration {
}
