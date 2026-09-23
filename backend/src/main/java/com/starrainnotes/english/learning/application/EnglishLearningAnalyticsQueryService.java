package com.starrainnotes.english.learning.application;

import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.learning.dto.AdminLearningAnalyticsView;
import com.starrainnotes.english.learning.dto.AdminLearningModuleView;
import com.starrainnotes.english.learning.dto.AdminLearningOverviewView;
import com.starrainnotes.english.learning.dto.AdminLearningTrendDayView;
import com.starrainnotes.english.learning.infrastructure.LearningAnalyticsRepository;
import com.starrainnotes.english.shared.content.EnglishContentRegistry;
import com.starrainnotes.english.shared.content.EnglishContentType;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class EnglishLearningAnalyticsQueryService {
    private static final List<String> TYPES = List.of("GRAMMAR", "READING", "LISTENING", "WRITING");
    private static final Set<Integer> RANGES = Set.of(7, 30, 90);
    private static final DateTimeFormatter GENERATED_AT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final LearningAnalyticsRepository repository;
    private final EnglishContentRegistry content;
    private final SiteSettingsTimezone timezone;

    public EnglishLearningAnalyticsQueryService(LearningAnalyticsRepository repository,
                                                EnglishContentRegistry content,
                                                SiteSettingsTimezone timezone) {
        this.repository = repository;
        this.content = content;
        this.timezone = timezone;
    }

    public AdminLearningAnalyticsView analytics(int days, String rawType) {
        if (!RANGES.contains(days)) throw invalid("days must be one of 7, 30 or 90.");
        String type = rawType == null ? "ALL" : rawType.trim().toUpperCase(Locale.ROOT);
        if (!"ALL".equals(type) && !TYPES.contains(type)) {
            throw invalid("type must be ALL, GRAMMAR, READING, LISTENING or WRITING.");
        }

        ZoneId zone = timezone.zone();
        LocalDate first = LocalDate.now(zone).minusDays(days - 1L);
        LocalDateTime sinceUtc = first.atStartOfDay(zone).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        String offset = zone.getRules().getOffset(Instant.now()).getId();
        if ("Z".equals(offset)) offset = "+00:00";

        return new AdminLearningAnalyticsView(days, type, ZonedDateTime.now(zone).format(GENERATED_AT),
                overview(sinceUtc, type), trend(first, days, sinceUtc, type, offset), modules(sinceUtc, type));
    }

    private AdminLearningOverviewView overview(LocalDateTime since, String type) {
        LearningAnalyticsRepository.Overview row = repository.overview(since, type);
        return new AdminLearningOverviewView(row.activeLearners(), row.attempts(), row.completions(),
                rate(row.completions(), row.attempts()), row.timeSpent());
    }

    private List<AdminLearningTrendDayView> trend(LocalDate first, int days, LocalDateTime since,
                                                   String type, String offset) {
        Map<LocalDate, LearningAnalyticsRepository.Trend> values = repository.trend(since, type, offset);
        List<AdminLearningTrendDayView> result = new ArrayList<>(days);
        for (int index = 0; index < days; index++) {
            LocalDate day = first.plusDays(index);
            LearningAnalyticsRepository.Trend row = values.getOrDefault(day,
                    new LearningAnalyticsRepository.Trend(0, 0, 0, 0));
            result.add(new AdminLearningTrendDayView(day.toString(), row.attempts(), row.completions(),
                    row.activeLearners(), row.timeSpent()));
        }
        return List.copyOf(result);
    }

    private List<AdminLearningModuleView> modules(LocalDateTime since, String selectedType) {
        Map<String, LearningAnalyticsRepository.Module> activity = repository.modules(since, selectedType);
        List<String> visibleTypes = "ALL".equals(selectedType) ? TYPES : List.of(selectedType);
        return visibleTypes.stream().map(type -> {
            LearningAnalyticsRepository.Module row =
                    activity.getOrDefault(type, LearningAnalyticsRepository.Module.EMPTY);
            long published = content.publishedCount(EnglishContentType.valueOf(type));
            return new AdminLearningModuleView(type, published, row.engagedContent(),
                    row.attempts(), row.completions(), rate(row.completions(), row.attempts()), row.timeSpent());
        }).toList();
    }

    private BigDecimal rate(long completed, long attempts) {
        if (attempts == 0) return BigDecimal.ZERO.setScale(1);
        return BigDecimal.valueOf(completed * 100).divide(BigDecimal.valueOf(attempts), 1, RoundingMode.HALF_UP);
    }

    private ApiException invalid(String detail) {
        return new ApiException(HttpStatus.BAD_REQUEST, "ENGLISH_ANALYTICS_FILTER_INVALID",
                "Invalid analytics filter", detail);
    }
}
