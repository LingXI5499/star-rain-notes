package com.starrainnotes.english.learning.application;

import com.starrainnotes.english.learning.dto.LearningInsightsView;
import com.starrainnotes.english.learning.dto.LearningActivityDayView;
import com.starrainnotes.english.learning.dto.LearningSummaryView;
import com.starrainnotes.english.learning.infrastructure.LearningInsightRepository;
import com.starrainnotes.english.learning.service.EnglishRecommendationService;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class LearningInsightQueryService {
    private final LearnerProfileService profiles;
    private final LearningInsightRepository repository;
    private final EnglishRecommendationService recommendations;
    public LearningInsightQueryService(LearnerProfileService profiles,LearningInsightRepository repository,
                                       EnglishRecommendationService recommendations) {
        this.profiles=profiles; this.repository=repository; this.recommendations=recommendations;
    }
    public LearningSummaryView summary(long accountId) {
        Long profile=profiles.profileId(accountId);
        return profile==null?new LearningSummaryView(0,0,0,0,Map.of(),List.of()):repository.summary(profile);
    }
    public LearningInsightsView insights(long accountId) {
        Long profile=profiles.profileId(accountId);
        if (profile==null) return new LearningInsightsView(0,0,0,0,null,null,List.of(),List.of(),List.of());
        Map<String,Object> totals=repository.totals(profile);
        List<LearningActivityDayView> activity=repository.activity(profile);
        return new LearningInsightsView(number(totals.get("total_time")),number(totals.get("total_attempts")),
                (int)activity.stream().filter(day->day.attempts()>0).count(),streak(activity),
                decimal(totals.get("average_score")),decimal(totals.get("average_mastery")),
                activity,repository.modules(profile),recommendations.recommendations(profile));
    }
    private int streak(List<LearningActivityDayView> activity) {
        int index=activity.size()-1;
        if(index<0) return 0;
        if(activity.get(index).attempts()==0) index--;
        int value=0;
        for(;index>=0&&activity.get(index).attempts()>0;index--) value++;
        return value;
    }
    private long number(Object value) { return value instanceof Number n?n.longValue():0; }
    private BigDecimal decimal(Object value) {
        return value instanceof BigDecimal d?d:value instanceof Number n?BigDecimal.valueOf(n.doubleValue()):null;
    }
}
