package com.starrainnotes.english.learning.application;

import com.starrainnotes.english.learning.domain.RecommendationEngine;
import com.starrainnotes.english.learning.dto.LearningRecommendationView;
import com.starrainnotes.english.learning.infrastructure.RecommendationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class RecommendationQueryService {
    private final RecommendationRepository repository;
    private final RecommendationEngine engine;

    public RecommendationQueryService(RecommendationRepository repository, RecommendationEngine engine) {
        this.repository = repository;
        this.engine = engine;
    }

    public List<LearningRecommendationView> recommendations(long learnerId) {
        return engine.select(repository.reviewAndContinue(learnerId),
                repository.bundleNextSteps(learnerId),
                repository.pairedContent(learnerId),
                repository.tagMatches(learnerId),
                () -> repository.starters(learnerId));
    }
}
