package com.starrainnotes.tutorial.properties;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "star-rain.learning.review")
public class LearningReviewProperties {
    private List<Integer> baseIntervalDays = List.of(1, 3, 7, 14, 30, 60);
}
