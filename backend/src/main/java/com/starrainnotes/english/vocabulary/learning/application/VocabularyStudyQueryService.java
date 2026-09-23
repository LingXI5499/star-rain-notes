package com.starrainnotes.english.vocabulary.learning.application;

import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.vocabulary.learning.*;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import com.starrainnotes.english.vocabulary.dto.VocabularyWordView;
import com.starrainnotes.english.vocabulary.application.VocabularyQueryService;
import com.starrainnotes.english.vocabulary.learning.infrastructure.VocabularyStudyRepository;
import com.starrainnotes.english.vocabulary.learning.infrastructure.VocabularyReviewLogRepository;
import com.starrainnotes.english.vocabulary.learning.domain.StudyDirectionPolicy;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class VocabularyStudyQueryService {

    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private final StudyDirectionPolicy directionPolicy = new StudyDirectionPolicy();

    private final VocabularyStudyRepository repository;
    private final VocabularyReviewLogRepository reviewLogs;
    private final VocabularyQueryService vocabulary;
    private final SiteSettingsTimezone timezone;

    public VocabularyStudyQueryService(VocabularyQueryService vocabulary, SiteSettingsTimezone timezone,
                                       VocabularyStudyRepository repository, VocabularyReviewLogRepository reviewLogs) {
        this.repository = repository;
        this.reviewLogs = reviewLogs;
        this.vocabulary = vocabulary;
        this.timezone = timezone;
    }

    public VocabularyStudySettingsView settings(long accountId) { return repository.settings(accountId); }

    public List<Map<String,Object>> memorySnapshot(long accountId) { return repository.memorySnapshot(accountId); }

    public List<VocabularyMemoryView> memories(long accountId, List<Long> wordIds) {
        if(wordIds==null||wordIds.isEmpty()) return List.of();
        List<Long> ids=wordIds.stream().filter(java.util.Objects::nonNull).distinct().limit(500).toList();
        return repository.memories(accountId,ids);
    }

    public VocabularyQueueView queue(long accountId, Long themeId) {
        VocabularyStudySettingsView setting = settings(accountId);
        LocalDateTime now = utcNow();
        LocalDateTime todayStart = LocalDate.now(siteZone()).atStartOfDay(siteZone())
                .withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        int dueCount = repository.dueCount(accountId, now);
        List<Long> dueIds = repository.dueIds(accountId, now, setting.dailyReviewLimit());

        List<Long> newIds = List.of();
        int introducedToday = repository.introducedSince(accountId, todayStart);
        int remainingNewLimit = Math.max(0, setting.dailyNewLimit() - introducedToday);
        if (themeId != null && remainingNewLimit > 0) {
            newIds = repository.newWordIds(themeId, accountId, remainingNewLimit);
        }
        List<Long> orderedIds = new ArrayList<>(dueIds);
        for (Long id : newIds) if (!orderedIds.contains(id)) orderedIds.add(id);
        List<VocabularyWordView> words = vocabulary.getWords(orderedIds);
        Map<Long, VocabularyMemoryView> states = new HashMap<>();
        for (VocabularyMemoryView state : memories(accountId, orderedIds)) states.put(state.wordId(), state);
        Map<Long, VocabularyWordView> wordById = new LinkedHashMap<>();
        for (VocabularyWordView word : words) wordById.put(word.id(), word);
        long day = LocalDate.now(siteZone()).toEpochDay();
        List<VocabularyStudyCardView> items = new ArrayList<>();
        for (Long id : orderedIds) {
            VocabularyWordView word = wordById.get(id);
            if (word == null) continue;
            boolean isNew = !states.containsKey(id) || "NEW".equals(states.get(id).learningStatus());
            VocabularyMemoryView state = states.getOrDefault(id,
                    new VocabularyMemoryView(id, 0, 0, 0, null, null, null, "NEW", null));
            String direction = directionPolicy.direction(setting.reviewDirection(), id, day);
            items.add(new VocabularyStudyCardView(word, state, direction, isNew));
        }
        return new VocabularyQueueView(items, dueCount, newIds.size(), format(now));
    }

    public VocabularyProgressView progress(long accountId) {
        LocalDateTime now = utcNow();
        LocalDateTime start = LocalDate.now(siteZone()).atStartOfDay(siteZone())
                .withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        long active = repository.activeCount(accountId);
        long due = repository.dueCountLong(accountId, now);
        long today = reviewLogs.countSince(accountId, start);
        long reviews = reviewLogs.count(accountId);
        long memoryCount = repository.totalMemoryCount(accountId);
        List<VocabularyReviewHistoryView> recent = reviewLogs.recent(accountId);
        return new VocabularyProgressView(active, due, today, reviews, memoryCount, recent);
    }

    public VocabularyMemoryView memory(long accountId,long wordId) { return repository.memory(accountId,wordId); }

    public VocabularyMemoryView optionalMemory(long accountId, long wordId, String displayMode) {
        try { return memory(accountId, wordId); }
        catch (ApiException ex) { return new VocabularyMemoryView(wordId, 0, 0, 0, null, null, null, "NEW", displayMode); }
    }

    private LocalDateTime utcNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private ZoneId siteZone() {
        return timezone.zone();
    }

    private String format(LocalDateTime value) {
        return value == null ? null : timezone.atSite(value).format(ISO_OFFSET);
    }
}
