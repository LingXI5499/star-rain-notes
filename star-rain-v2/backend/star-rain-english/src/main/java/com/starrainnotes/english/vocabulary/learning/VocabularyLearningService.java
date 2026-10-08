package com.starrainnotes.english.vocabulary.learning;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.english.vocabulary.dto.VocabularyDto.Page;
import com.starrainnotes.english.vocabulary.dto.VocabularyDto.Word;
import com.starrainnotes.english.vocabulary.learning.VocabularyLearningModels.*;
import com.starrainnotes.english.vocabulary.learning.VocabularyLearningModels.Queue;
import com.starrainnotes.english.vocabulary.mapper.VocabularyLearningMapper;
import com.starrainnotes.english.vocabulary.mapper.VocabularyMapper;
import com.starrainnotes.english.vocabulary.service.VocabularyService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Plan and permanent memories share command locks, never deletion lifecycles. */
@Service
@Transactional(readOnly = true)
public class VocabularyLearningService {
    private static final int SELECTION_CHUNK = 500;
    private static final int MAX_PLAN_WORDS = 100_000;
    private final VocabularyLearningMapper mapper;
    private final VocabularyMapper content;
    private final VocabularyService vocabulary;
    private final ObjectMapper json;
    private final Clock clock;

    public VocabularyLearningService(VocabularyLearningMapper mapper, VocabularyMapper content,
            VocabularyService vocabulary, ObjectMapper json, @Qualifier("vocabularyLearningClock") Clock clock) {
        this.mapper = mapper;
        this.content = content;
        this.vocabulary = vocabulary;
        this.json = json;
        this.clock = clock;
    }

    public Plan plan(long accountId) {
        Plan plan = mapper.plan(accountId);
        if (plan == null) { return new Plan(); }
        if (!"NONE".equals(plan.getStatus())) { plan.setNextDefaultEntry(mapper.defaultEntry(accountId)); }
        return plan;
    }

    public Page<Word> words(long accountId, Filter filter, int page, int size) {
        pagination(page, size, 100);
        long total = mapper.countWords(accountId, filter);
        List<Long> ids = mapper.wordPage(accountId, filter, (long) (page - 1) * size, size);
        return Page.<Word>builder().items(orderedWords(ids)).total(total).page(page).size(size)
                .totalPages((int) ((total + size - 1) / size)).build();
    }

    public List<State> states(long accountId, List<Long> ids) {
        if (ids.isEmpty()) { return List.of(); }
        if (ids.size() > 100 || ids.stream().anyMatch(id -> id == null || id <= 0)) { throw invalid("一次最多读取 100 个有效词条"); }
        List<State> states = mapper.states(accountId, ids);
        Map<Long, List<ModeMemory>> modes = mapper.modes(accountId, ids).stream().collect(Collectors.groupingBy(ModeMemory::getWordId));
        for (State state : states) { state.setModeMemory(modes.getOrDefault(state.getWordId(), List.of())); }
        return states;
    }

    /** Preview only reads IDs and one small content page; it creates no memory or plan. */
    public Preview preview(long accountId, Selection selection, int page, int size) {
        pagination(page, size, 100);
        Plan current = plan(accountId);
        Expanded expanded = expand(accountId, selection, current.getRevision());
        Preview preview = new Preview();
        preview.setExpectedRevision(current.getRevision());
        preview.setExistingPlan(current);
        preview.setPreviewFingerprint(expanded.getFingerprint());
        preview.setTotalWords(expanded.getIds().size());
        preview.setTotalGroups((expanded.getIds().size() + selection.getBatchSize() - 1) / selection.getBatchSize());
        preview.setSourceName(sourceName(selection));
        long learned = 0;
        for (int i = 0; i < expanded.getIds().size(); i += SELECTION_CHUNK) {
            learned += mapper.learnedCount(accountId, expanded.getIds().subList(i, Math.min(i + SELECTION_CHUNK, expanded.getIds().size())));
        }
        preview.setLearnedWords(learned);
        int start = (int) Math.min(expanded.getIds().size(), (long) (page - 1) * size);
        preview.setItems(orderedWords(expanded.getIds().subList(start, Math.min(expanded.getIds().size(), start + size))));
        LinkedHashSet<Long> sampleIds = new LinkedHashSet<>(expanded.getIds().subList(0, Math.min(3, expanded.getIds().size())));
        sampleIds.addAll(expanded.getIds().subList(Math.max(0, expanded.getIds().size() - 3), expanded.getIds().size()));
        preview.setSamples(orderedWords(List.copyOf(sampleIds)));
        preview.setPage(page);
        preview.setTotalPages((expanded.getIds().size() + size - 1) / size);
        return preview;
    }

    @Transactional
    public Plan confirm(long accountId, Selection selection) {
        Plan slot = lockSlot(accountId);
        requireRevision(slot, selection.getExpectedRevision());
        Expanded expanded = expand(accountId, selection, slot.getRevision());
        if (selection.getPreviewFingerprint() == null || !selection.getPreviewFingerprint().equals(expanded.getFingerprint())) {
            throw conflict("选词条件或词库已变化，请重新预览确认");
        }
        if (expanded.getIds().isEmpty()) { throw invalid("请至少选择一个单词"); }
        Plan replacement = new Plan();
        replacement.setName(selection.getName() == null || selection.getName().isBlank() ? sourceName(selection) : selection.getName().trim());
        replacement.setSourceThemeId(selection.getThemeId());
        replacement.setSourceName(sourceName(selection));
        replacement.setBatchSize(selection.getBatchSize());
        replacement.setTotalWords(expanded.getIds().size());
        replacement.setCreatedAt(now());
        mapper.clearItems(accountId);
        mapper.replacePlan(accountId, replacement);
        for (int start = 0; start < expanded.getIds().size(); start += SELECTION_CHUNK) {
            List<Long> ids = expanded.getIds().subList(start, Math.min(expanded.getIds().size(), start + SELECTION_CHUNK));
            // Only IDs are expanded in memory. Content snapshots are fetched and inserted in bounded chunks.
            Map<Long, Word> words = vocabulary.wordsByIds(ids, SELECTION_CHUNK).stream()
                    .collect(Collectors.toMap(Word::getId, Function.identity()));
            List<PlanItem> items = new ArrayList<>(ids.size());
            for (int i = 0; i < ids.size(); i++) {
                Word word = words.get(ids.get(i));
                if (word == null) { throw conflict("词条已失效，请重新预览"); }
                PlanItem item = new PlanItem();
                item.setWordId(word.getId());
                item.setSortNo(start + i + 1);
                item.setGroupNo((start + i) / selection.getBatchSize() + 1);
                item.setWordJson(writeJson(word));
                items.add(item);
            }
            mapper.insertItems(accountId, items);
        }
        return plan(accountId);
    }

    public List<PlanItem> items(long accountId, long revision, Integer groupNo, int after, int limit) {
        requireRevision(plan(accountId), revision);
        if (after < 0 || limit < 1 || limit > 100 || (groupNo != null && groupNo < 1)) { throw invalid("无效的分组或分页参数"); }
        List<PlanItem> items = mapper.items(accountId, groupNo, after, limit);
        Map<Long, State> states = states(accountId, items.stream().map(PlanItem::getWordId).toList()).stream()
                .collect(Collectors.toMap(State::getWordId, Function.identity()));
        for (PlanItem item : items) {
            item.setWord(readJson(item.getWordJson(), Word.class));
            item.setMemory(states.get(item.getWordId()));
        }
        return items;
    }

    public List<GroupProgress> groups(long accountId, long revision, int after, int limit) {
        requireRevision(plan(accountId), revision);
        if (after < 0 || limit < 1 || limit > 100) { throw invalid("无效的分组分页参数"); }
        return mapper.groups(accountId, after, limit);
    }

    @Transactional
    public Plan resize(long accountId, RevisionRequest request) {
        Plan slot = lockSlot(accountId);
        requireRevision(slot, request.getExpectedRevision());
        if ("NONE".equals(slot.getStatus())) { throw conflict("当前没有学习计划"); }
        int frozen = mapper.lastStartedGroup(accountId);
        Integer firstSort = mapper.firstRemainingSort(accountId, frozen);
        if (firstSort != null) { mapper.regroup(accountId, frozen, firstSort, request.getBatchSize()); }
        mapper.resize(accountId, request.getBatchSize(), now());
        return plan(accountId);
    }

    @Transactional
    public Plan cancel(long accountId, long revision) {
        Plan slot = lockSlot(accountId);
        requireRevision(slot, revision);
        mapper.clearItems(accountId);
        mapper.cancelPlan(accountId, now());
        return plan(accountId);
    }

    /** A deleted content word remains visible in the snapshot, and can be skipped without a fake rating. */
    @Transactional
    public Plan skipMissing(long accountId, long wordId, long revision) {
        requireRevision(lockSlot(accountId), revision);
        if (mapper.item(accountId, wordId) == null || content.word(wordId) != null) { throw invalid("只能跳过当前计划中已失效的词条"); }
        mapper.markDone(accountId, wordId, 7, now());
        mapper.updatePlanStatus(accountId, now());
        return plan(accountId);
    }

    @Transactional
    public RatingResult rate(long accountId, long wordId, RatingRequest request) {
        validateRating(request);
        Plan slot = lockSlot(accountId);
        RatingLog previousLog = mapper.log(accountId, request.getReviewSessionId().toLowerCase(Locale.ROOT));
        if (previousLog != null) { return replay(wordId, request, previousLog); }
        PlanItem planItem = null;
        if ("PLAN".equals(request.getSource())) {
            if (request.getPlanRevision() == null) { throw invalid("计划评分必须携带修订号"); }
            requireRevision(slot, request.getPlanRevision());
            planItem = mapper.item(accountId, wordId);
            if (planItem == null || "NONE".equals(slot.getStatus())) { throw conflict("此词不属于当前计划，请刷新"); }
        } else if (request.getPlanRevision() != null) { throw invalid("独立复习不接受计划修订号"); }
        vocabulary.word(String.valueOf(wordId));
        LocalDateTime now = now();
        mapper.initializeMemory(accountId, wordId, now);
        State oldMemory = mapper.lockMemory(accountId, wordId);
        List<ModeMemory> modes = new ArrayList<>(mapper.modes(accountId, List.of(wordId)));
        ModeMemory mode = modes.stream().filter(item -> request.getDirection().equals(item.getDirection())).findFirst().orElse(null);
        if ("REVIEW".equals(request.getSource()) && mode == null) { throw invalid("此方向尚未学习，请从学习计划开始"); }
        if (mode == null) {
            mode = new ModeMemory();
            mode.setWordId(wordId);
            mode.setDirection(request.getDirection());
            modes.add(mode);
        }
        RatingLog log = new RatingLog();
        log.setWordId(wordId);
        log.setReviewSessionId(request.getReviewSessionId().toLowerCase(Locale.ROOT));
        log.setDirection(request.getDirection());
        log.setRating(request.getRating());
        log.setSource(request.getSource());
        log.setPlanRevision(request.getPlanRevision());
        log.setAudioPlayed(request.isAudioPlayed());
        log.setOldStep(mode.getReviewStep());
        log.setOldScore(mode.getEmaScore());
        log.setScheduledAt(mode.getNextReviewAt());
        log.setReviewedAt(now);
        log.setTimingStatus(VocabularyHighIntensityReviewPolicy.timing(mode.getNextReviewAt(), now));
        mode.setEmaScore(VocabularyMasteryCalculator.updatedEma(mode.getRatingCount(), mode.getEmaScore(), request.getRating()));
        mode.setRatingCount(mode.getRatingCount() + 1);
        mode.setLastRating(request.getRating());
        mode.setRecentGoodCount("KNOW".equals(request.getRating()) ? mode.getRecentGoodCount() + 1 : 0);
        mode.setFirstRatedAt(mode.getFirstRatedAt() == null ? now : mode.getFirstRatedAt());
        mode.setLastRatedAt(now);
        mode.setAudioVerified(mode.isAudioVerified() || ("AUDIO_TO_BOTH".equals(mode.getDirection()) && request.isAudioPlayed()));
        LocalDate day = VocabularyMasteryCalculator.siteDay(now);
        mapper.addDay(accountId, wordId, day, request.getDirection());
        DayCounts days = mapper.days(accountId, wordId, day.minusDays(6), day);
        LocalDateTime first = oldMemory.getFirstRatedAt() == null ? now : oldMemory.getFirstRatedAt();
        // Once graduated, successful consolidation remains at 35 days; the entrance gate applies to graduation only.
        boolean graduated = "KNOW".equals(request.getRating()) && ("MASTERED".equals(oldMemory.getMasteryRank())
                || VocabularyMasteryCalculator.graduated(modes, first, now, days.getTotal(), days.getRecent()));
        int step = VocabularyHighIntensityReviewPolicy.nextStep(log.getOldStep(), request.getRating(), graduated);
        long interval = VocabularyHighIntensityReviewPolicy.intervalSeconds(step);
        mode.setReviewStep(step);
        mode.setNextReviewAt(now.plusSeconds(interval));
        mapper.saveMode(accountId, mode);
        if (!graduated) { mapper.exitConsolidation(accountId, wordId, now.plusDays(28)); }
        int score = VocabularyMasteryCalculator.score(modes);
        String rank = VocabularyMasteryCalculator.rank(score, graduated, true);
        mapper.saveMastery(accountId, wordId, score, rank, request.getRating(), now);
        log.setNewStep(step);
        log.setNewScore(mode.getEmaScore());
        log.setReviewNumber(mode.getRatingCount());
        log.setIntervalSeconds(interval);
        mapper.insertLog(accountId, log);
        boolean completed = false;
        if (planItem != null) {
            int bit = 1 << VocabularyMasteryCalculator.DIRECTIONS.indexOf(request.getDirection());
            completed = (planItem.getDoneMask() & bit) == 0;
            mapper.markDone(accountId, wordId, bit, now);
            mapper.updatePlanStatus(accountId, now);
        }
        RatingResult result = new RatingResult();
        result.setMemory(states(accountId, List.of(wordId)).getFirst());
        result.setModeMemory(mode);
        result.setPlanItemCompleted(completed);
        result.setPlan(planItem == null ? null : plan(accountId));
        result.setIntervalSeconds(interval);
        result.setTimingStatus(log.getTimingStatus());
        mapper.saveLogResult(accountId, log.getReviewSessionId(), writeJson(result));
        return result;
    }

    public Queue queue(long accountId, int limit, String encodedCursor) {
        if (limit < 1 || limit > 50) { throw invalid("复习批次为 1～50 张卡片"); }
        DueCursor cursor = new DueCursor();
        cursor.setAsOf(now());
        if (encodedCursor != null && !encodedCursor.isBlank()) {
            try {
                if (encodedCursor.length() > 1000) { throw invalid("无效的复习游标"); }
                cursor = readJson(new String(Base64.getUrlDecoder().decode(encodedCursor), StandardCharsets.UTF_8), DueCursor.class);
                if (cursor.getAsOf() == null || cursor.getDueAt() == null || cursor.getDirection() == null || cursor.getWordId() <= 0
                        || !VocabularyMasteryCalculator.DIRECTIONS.contains(cursor.getDirection()) || cursor.getAsOf().isAfter(now())) {
                    throw invalid("无效的复习游标");
                }
            } catch (IllegalArgumentException | IllegalStateException exception) { throw invalid("无效的复习游标"); }
        }
        List<ModeMemory> due = mapper.due(accountId, cursor, limit + 1);
        boolean hasNext = due.size() > limit;
        if (hasNext) { due = due.subList(0, limit); }
        List<Long> ids = due.stream().map(ModeMemory::getWordId).distinct().toList();
        Map<Long, Word> words = orderedWords(ids).stream().collect(Collectors.toMap(Word::getId, Function.identity()));
        Map<Long, State> states = states(accountId, ids).stream().collect(Collectors.toMap(State::getWordId, Function.identity()));
        List<DueCard> cards = new ArrayList<>();
        for (ModeMemory mode : due) {
            DueCard card = new DueCard();
            card.setWord(words.get(mode.getWordId()));
            card.setMemory(states.get(mode.getWordId()));
            card.setModeMemory(mode);
            card.setDirection(mode.getDirection());
            card.setNextReviewAt(mode.getNextReviewAt());
            cards.add(card);
        }
        Queue queue = new Queue();
        queue.setItems(cards);
        queue.setAsOf(cursor.getAsOf());
        if (hasNext) {
            ModeMemory last = due.getLast();
            cursor.setDueAt(last.getNextReviewAt());
            cursor.setWordId(last.getWordId());
            cursor.setDirection(last.getDirection());
            queue.setNextCursor(Base64.getUrlEncoder().withoutPadding().encodeToString(writeJson(cursor).getBytes(StandardCharsets.UTF_8)));
        }
        return queue;
    }

    public ReviewSummary reviewSummary(long accountId) {
        LocalDateTime now = now();
        LocalDateTime today = VocabularyMasteryCalculator.siteDay(now).atStartOfDay(com.starrainnotes.english.vocabulary.utils.VocabularySiteTime.SITE_ZONE)
                .withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        return mapper.reviewSummary(accountId, now, today);
    }

    private RatingResult replay(long wordId, RatingRequest request, RatingLog log) {
        if (!Objects.equals(log.getWordId(), wordId) || !Objects.equals(log.getDirection(), request.getDirection())
                || !Objects.equals(log.getRating(), request.getRating()) || !Objects.equals(log.getSource(), request.getSource())
                || !Objects.equals(log.getPlanRevision(), request.getPlanRevision()) || log.isAudioPlayed() != request.isAudioPlayed()
                || log.getResultJson() == null) {
            throw conflict("同一复习标识不能用于不同评分，请刷新后重试");
        }
        RatingResult result = readJson(log.getResultJson(), RatingResult.class);
        result.setDuplicate(true);
        return result;
    }

    private Expanded expand(long accountId, Selection selection, long revision) {
        if (selection.getBatchSize() < 5 || selection.getBatchSize() > 100 || selection.getWordIds() == null
                || selection.getExcludedWordIds() == null || selection.getWordIds().size() > 1000 || selection.getExcludedWordIds().size() > 1000) {
            throw invalid("组大小为 5～100，手工选词和排除清单各最多 1000 项；大词库请使用筛选全选");
        }
        MessageDigest digest;
        try { digest = MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
        digest.update((accountId + ":" + revision + ":" + selection.getBatchSize()).getBytes(StandardCharsets.UTF_8));
        List<Long> ids = new ArrayList<>();
        long after = 0;
        for (;;) {
            List<SelectionRow> chunk = mapper.selected(accountId, selection, after, SELECTION_CHUNK);
            for (SelectionRow row : chunk) {
                digest.update((":" + row.getWordId() + ":" + row.getToken()).getBytes(StandardCharsets.UTF_8));
                ids.add(row.getWordId());
            }
            if (ids.size() > MAX_PLAN_WORDS) { throw invalid("单份计划最多 100000 词，请缩小筛选范围"); }
            if (chunk.size() < SELECTION_CHUNK) { break; }
            after = chunk.getLast().getWordId();
        }
        return new Expanded(ids, HexFormat.of().formatHex(digest.digest()));
    }

    private List<Word> orderedWords(List<Long> ids) {
        if (ids.isEmpty()) { return List.of(); }
        Map<Long, Word> words = vocabulary.wordsByIds(ids, ids.size()).stream().collect(Collectors.toMap(Word::getId, Function.identity()));
        return ids.stream().map(words::get).filter(Objects::nonNull).toList();
    }

    private String sourceName(Selection selection) {
        if (selection.getThemeId() == null) { return "自选词汇"; }
        var theme = content.theme(selection.getThemeId());
        return theme == null ? "主题词汇" : theme.getName();
    }

    private Plan lockSlot(long accountId) {
        mapper.ensureSlot(accountId);
        return mapper.lockSlot(accountId);
    }

    private void requireRevision(Plan plan, long revision) {
        if (plan.getRevision() != revision) { throw conflict("学习计划已在另一页面更改，请刷新后重试"); }
    }

    private void validateRating(RatingRequest request) {
        if (request.getDirection() == null || request.getRating() == null || request.getSource() == null
                || !VocabularyMasteryCalculator.DIRECTIONS.contains(request.getDirection())
                || !Set.of("FORGOT", "UNCERTAIN", "KNOW").contains(request.getRating())
                || !Set.of("PLAN", "REVIEW", "FREE").contains(request.getSource())
                || request.getReviewSessionId() == null || !request.getReviewSessionId().matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
            throw invalid("请提供有效的评分、方向、来源和复习标识");
        }
        if ("AUDIO_TO_BOTH".equals(request.getDirection()) && !request.isAudioPlayed()) {
            throw invalid("听音评分前必须实际播放发音；无法播放时请重试或切换方向");
        }
    }

    private void pagination(int page, int size, int max) {
        if (page < 1 || size < 1 || size > max) { throw invalid("无效的分页参数"); }
    }

    private LocalDateTime now() { return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS); }
    private String writeJson(Object value) {
        try { return json.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Cannot encode vocabulary snapshot", exception); }
    }
    private <T> T readJson(String value, Class<T> type) {
        try { return json.readValue(value, type); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Cannot decode vocabulary snapshot", exception); }
    }
    private ApiException invalid(String message) { return new ApiException("ENGLISH_VOCABULARY_LEARNING_INVALID", message, 400); }
    private ApiException conflict(String message) { return new ApiException("ENGLISH_VOCABULARY_LEARNING_CONFLICT", message, 409); }
    @lombok.Value
    private static class Expanded {
        List<Long> ids;
        String fingerprint;
    }
}
