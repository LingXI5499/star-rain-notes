package com.starrainnotes.boot;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.english.vocabulary.dto.VocabularyDto.Word;
import com.starrainnotes.english.vocabulary.learning.*;
import com.starrainnotes.english.vocabulary.learning.VocabularyLearningModels.*;
import com.starrainnotes.english.vocabulary.mapper.*;
import com.starrainnotes.english.vocabulary.service.impl.VocabularyServiceImpl;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/** Real MySQL probes use a disposable schema; the application's existing database is never migrated. */
class VocabularyIntensiveLearningIntegrationTest extends MapperXmlIntegrationSupport {
    private SqlSession sql;
    private String original;
    private String schema;
    private VocabularyLearningService service;
    private VocabularyLearningMapper mapper;
    private final MutableClock clock = new MutableClock();
    private static final long ACCOUNT = 41;

    @BeforeEach void setup() throws Exception {
        sql = openSession();
        Connection connection = sql.getConnection(); original = connection.getCatalog();
        schema = "sr_vocab_probe_" + Long.toUnsignedString(System.nanoTime());
        assertTrue(schema.matches("sr_vocab_probe_[0-9]+"));
        try (Statement admin = connection.createStatement()) { admin.execute("CREATE DATABASE " + schema + " CHARACTER SET utf8mb4"); }
        connection.setCatalog(schema);
        try (Statement statement = connection.createStatement()) { statement.execute("SET time_zone = '+00:00'"); }
        apply("V2_001__account.sql"); apply("V2_020__english_content.sql"); apply("V2_022__english_vocabulary_memory.sql");
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO sr_english_vocabulary_theme(id,layer,name) VALUES(1,'Probe','Probe words')");
        }
        addWords(1, 45);
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO sr_english_vocabulary_memory(account_id,word_id,memory_count,review_count,review_step,learning_status,next_review_at) VALUES(41,45,19,11,10,'ACTIVE','2026-09-01')");
            statement.executeUpdate("INSERT INTO sr_english_vocabulary_review_log(account_id,word_id,review_session_id,direction,review_number,reviewed_at,interval_seconds,timing_status) VALUES(41,45,'00000000-0000-4000-8000-000000000045','EN_TO_ZH',11,'2026-09-01',5184000,'ON_TIME')");
            statement.executeUpdate("INSERT INTO sr_english_vocabulary_study_setting(account_id,review_direction) VALUES(41,'MIXED')");
        }
        sql.commit(true);
        apply("V2_034__vocabulary_intensive_learning.sql"); sql.commit(true);
        mapper = sql.getMapper(VocabularyLearningMapper.class); service = service(sql);
        clock.at("2026-10-08T00:00:00Z");
    }
    @AfterEach void cleanup() throws Exception {
        if (sql == null) { return; }
        try {
            sql.rollback(true); Connection connection = sql.getConnection(); connection.setCatalog(original);
            if (schema != null) {
                assertTrue(schema.matches("sr_vocab_probe_[0-9]+"));
                try (Statement statement = connection.createStatement()) { statement.execute("DROP DATABASE IF EXISTS " + schema); }
            }
        } finally { sql.close(); }
    }
    private VocabularyLearningService service(SqlSession session) {
        VocabularyMapper content = session.getMapper(VocabularyMapper.class);
        return new VocabularyLearningService(session.getMapper(VocabularyLearningMapper.class), content,
                new VocabularyServiceImpl(content, org.mockito.Mockito.mock(org.springframework.context.ApplicationEventPublisher.class)), new ObjectMapper().findAndRegisterModules(), clock);
    }
    private void apply(String file) throws SQLException {
        ScriptUtils.executeSqlScript(sql.getConnection(), new EncodedResource(new ClassPathResource("db/migration/" + file), "UTF-8"));
    }
    private void addWords(int first, int last) throws SQLException {
        try (PreparedStatement statement = sql.getConnection().prepareStatement("INSERT INTO sr_english_vocabulary_word(id,theme_id,word,translation,examples,sort_order) VALUES(?,1,?,'释义','[]',?)")) {
            for (int i = first; i <= last; i++) { statement.setInt(1, i); statement.setString(2, "probe" + i); statement.setInt(3, i); statement.addBatch(); }
            statement.executeBatch();
        }
    }
    private Selection all() { Selection selection = new Selection(); selection.setThemeId(1L); selection.setSelectAllMatched(true); return selection; }
    private Selection preview(Selection selection) {
        Preview preview = service.preview(ACCOUNT, selection, 1, 24);
        selection.setExpectedRevision(preview.getExpectedRevision()); selection.setPreviewFingerprint(preview.getPreviewFingerprint()); return selection;
    }
    private Plan confirmAll() { return service.confirm(ACCOUNT, preview(all())); }
    private RatingRequest rating(String direction, String rating, String source, Long revision) {
        RatingRequest request = new RatingRequest(); request.setReviewSessionId(UUID.randomUUID().toString()); request.setDirection(direction);
        request.setRating(rating); request.setSource(source); request.setPlanRevision(revision); request.setAudioPlayed("AUDIO_TO_BOTH".equals(direction)); return request;
    }
    private int count(String table) throws SQLException {
        assertTrue(Set.of("sr_english_vocabulary_memory", "sr_english_vocabulary_mode_memory", "sr_english_vocabulary_review_log", "sr_english_vocabulary_plan_item", "sr_english_vocabulary_rating_day").contains(table));
        try (Statement statement = sql.getConnection().createStatement(); ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + table)) { assertTrue(result.next()); return result.getInt(1); }
    }

    @Test void migrationAndPreviewRetainLegacyFactsWithoutFabricatedRatings() throws Exception {
        State legacy = service.states(ACCOUNT, List.of(45L)).getFirst(); assertEquals(19, legacy.getMemoryCount()); assertNull(legacy.getMasteryRank());
        assertEquals(0, legacy.getModeMemory().getFirst().getRatingCount()); assertEquals(1, count("sr_english_vocabulary_review_log"));
        assertEquals("EN_TO_ZH", sql.getMapper(VocabularyStudyMapper.class).settings(ACCOUNT).getReviewDirection());
        Preview preview = service.preview(ACCOUNT, all(), 1, 24); assertEquals(45, preview.getTotalWords()); assertEquals(3, preview.getTotalGroups()); assertEquals(1, preview.getLearnedWords()); assertEquals(24, preview.getItems().size());
        assertEquals(1, count("sr_english_vocabulary_memory")); assertEquals(0, count("sr_english_vocabulary_plan_item"));
        Plan plan = confirmAll(); assertEquals(1, plan.getRevision()); assertEquals(3, plan.getTotalGroups());
        assertEquals(20, service.items(ACCOUNT, 1, 1, 0, 100).size()); assertEquals(5, service.items(ACCOUNT, 1, 3, 0, 100).size());
        assertEquals(1, service.reviewSummary(ACCOUNT).getDueCards()); assertEquals(45L, service.queue(ACCOUNT, 20, null).getItems().getFirst().getWord().getId());
        assertEquals(1, count("sr_english_vocabulary_memory"));
    }

    @Test void threeRoundsAndDefaultEntryUseEarliestIncompleteModeAndGroup() {
        Plan plan = confirmAll();
        service.items(ACCOUNT, 1, 3, 0, 100); assertEquals(1, service.plan(ACCOUNT).getNextDefaultEntry().getGroupNo());
        for (long id = 1; id <= 20; id++) { service.rate(ACCOUNT, id, rating("EN_TO_ZH", "FORGOT", "PLAN", plan.getRevision())); }
        assertEquals("ZH_TO_EN", service.plan(ACCOUNT).getNextDefaultEntry().getDirection()); assertEquals(1, service.plan(ACCOUNT).getNextDefaultEntry().getGroupNo());
        clock.at("2026-10-08T00:01:00Z"); service.rate(ACCOUNT, 1, rating("ZH_TO_EN", "KNOW", "PLAN", 1L));
        clock.at("2026-10-08T00:02:00Z"); service.rate(ACCOUNT, 1, rating("AUDIO_TO_BOTH", "KNOW", "PLAN", 1L));
        State state = service.states(ACCOUNT, List.of(1L)).getFirst(); assertEquals(3, state.getMemoryCount()); assertEquals(3, state.getModeMemory().size());
        assertEquals(3, state.getModeMemory().stream().map(ModeMemory::getNextReviewAt).distinct().count());
        assertEquals("BEGINNER", state.getMasteryRank());
        for (long id = 2; id <= 20; id++) { service.rate(ACCOUNT, id, rating("ZH_TO_EN", "KNOW", "PLAN", 1L)); }
        assertEquals("AUDIO_TO_BOTH", service.plan(ACCOUNT).getNextDefaultEntry().getDirection());
        for (long id = 2; id <= 20; id++) { service.rate(ACCOUNT, id, rating("AUDIO_TO_BOTH", "KNOW", "PLAN", 1L)); }
        assertEquals(2, service.plan(ACCOUNT).getNextDefaultEntry().getGroupNo());
    }

    @Test void replacementRevisionAndDeletionNeverRemoveMemoryOrLogs() throws Exception {
        confirmAll(); service.rate(ACCOUNT, 1, rating("EN_TO_ZH", "KNOW", "PLAN", 1L));
        Selection selection = new Selection(); selection.setWordIds(List.of(2L)); preview(selection);
        Plan replaced = service.confirm(ACCOUNT, selection); assertEquals(2, replaced.getRevision()); assertEquals(1, count("sr_english_vocabulary_plan_item"));
        assertEquals(2, count("sr_english_vocabulary_review_log")); assertEquals(2, count("sr_english_vocabulary_memory"));
        assertThrows(ApiException.class, () -> service.rate(ACCOUNT, 1, rating("EN_TO_ZH", "KNOW", "PLAN", 1L)));
        assertThrows(ApiException.class, () -> service.confirm(ACCOUNT, selection));
        service.cancel(ACCOUNT, 2); assertEquals(3, service.plan(ACCOUNT).getRevision()); assertEquals("NONE", service.plan(ACCOUNT).getStatus());
        assertEquals(2, count("sr_english_vocabulary_review_log")); assertEquals(2, count("sr_english_vocabulary_mode_memory"));
        assertEquals(1, service.states(ACCOUNT, List.of(1L)).getFirst().getMemoryCount());
    }

    @Test void stableFingerprintIncludesWholeFilteredSelectionAndContentChanges() throws Exception {
        Selection selection = all(); selection.setExcludedWordIds(List.of(2L, 45L)); preview(selection);
        assertEquals(43, service.preview(ACCOUNT, selection, 2, 24).getTotalWords());
        try (Statement statement = sql.getConnection().createStatement()) { statement.executeUpdate("UPDATE sr_english_vocabulary_word SET translation='changed' WHERE id=44"); }
        assertThrows(ApiException.class, () -> service.confirm(ACCOUNT, selection));
        preview(selection); Plan plan = service.confirm(ACCOUNT, selection); assertEquals(43, plan.getTotalWords());
        assertNull(mapper.item(ACCOUNT, 2)); assertNotNull(mapper.item(ACCOUNT, 44));
        Filter filter = new Filter(); filter.setThemeId(1L); filter.setInPlan("YES"); assertEquals(43, service.words(ACCOUNT, filter, 1, 24).getTotal());
        filter.setLearned("YES"); assertEquals(0, service.words(ACCOUNT, filter, 1, 24).getTotal());
    }

    @Test void sameDaySpamCannotGraduateAndCrossDayGraduationConsolidatesUntilFailure() {
        for (int round = 0; round < 4; round++) for (String direction : VocabularyMasteryCalculator.DIRECTIONS) service.rate(ACCOUNT, 1, rating(direction, "KNOW", "FREE", null));
        assertEquals("SKILLED", service.states(ACCOUNT, List.of(1L)).getFirst().getMasteryRank());
        clock.at("2026-10-10T00:00:00Z"); for (String direction : VocabularyMasteryCalculator.DIRECTIONS) service.rate(ACCOUNT, 1, rating(direction, "KNOW", "FREE", null));
        clock.at("2026-10-15T00:00:00Z"); RatingResult graduation = null;
        for (String direction : VocabularyMasteryCalculator.DIRECTIONS) graduation = service.rate(ACCOUNT, 1, rating(direction, "KNOW", "FREE", null));
        assertNotNull(graduation); assertEquals("MASTERED", graduation.getMemory().getMasteryRank()); assertEquals(35L * 86400, graduation.getIntervalSeconds());
        clock.at("2026-11-19T00:00:00Z"); assertEquals(35L * 86400, service.rate(ACCOUNT, 1, rating("AUDIO_TO_BOTH", "KNOW", "FREE", null)).getIntervalSeconds());
        RatingResult failure = service.rate(ACCOUNT, 1, rating("AUDIO_TO_BOTH", "UNCERTAIN", "FREE", null)); assertEquals(3600, failure.getIntervalSeconds()); assertNotEquals("MASTERED", failure.getMemory().getMasteryRank());
        assertTrue(failure.getMemory().getModeMemory().stream().allMatch(mode -> mode.getReviewStep() <= 13));
        assertEquals(300, service.rate(ACCOUNT, 1, rating("AUDIO_TO_BOTH", "FORGOT", "FREE", null)).getIntervalSeconds());
    }

    @Test void idempotentReplayChecksEveryPayloadFieldAndPartialWritesRollback() throws Exception {
        RatingRequest request = rating("EN_TO_ZH", "KNOW", "FREE", null); RatingResult first = service.rate(ACCOUNT, 1, request); sql.commit();
        clock.at("2026-10-08T01:00:00Z"); RatingResult replay = service.rate(ACCOUNT, 1, request);
        assertTrue(replay.isDuplicate()); assertEquals(first.getModeMemory().getNextReviewAt(), replay.getModeMemory().getNextReviewAt());
        request.setRating("FORGOT"); assertThrows(ApiException.class, () -> service.rate(ACCOUNT, 1, request));
        assertEquals(2, count("sr_english_vocabulary_review_log"));
        assertThrows(ApiException.class, () -> service.rate(ACCOUNT, 3, rating("ZH_TO_EN", "KNOW", "REVIEW", null)));
        sql.rollback(); assertEquals(2, count("sr_english_vocabulary_memory"));
        assertNull(mapper.lockMemory(ACCOUNT, 3));
    }

    @Test void resizePreservesStartedGroupAndDeletedContentCanBeSkippedWithoutRating() throws Exception {
        confirmAll(); service.rate(ACCOUNT, 1, rating("EN_TO_ZH", "KNOW", "PLAN", 1L));
        RevisionRequest resize = new RevisionRequest(); resize.setExpectedRevision(1); resize.setBatchSize(10);
        Plan plan = service.resize(ACCOUNT, resize); assertEquals(2, plan.getRevision()); assertEquals(4, plan.getTotalGroups());
        assertEquals(20, service.items(ACCOUNT, 2, 1, 0, 100).size()); assertEquals(10, service.items(ACCOUNT, 2, 2, 0, 100).size()); assertEquals(5, service.items(ACCOUNT, 2, 4, 0, 100).size());
        try (Statement statement = sql.getConnection().createStatement()) { statement.executeUpdate("DELETE FROM sr_english_vocabulary_word WHERE id=2"); }
        sql.clearCache();
        assertTrue(service.items(ACCOUNT, 2, 1, 0, 100).get(1).isUnavailable());
        service.skipMissing(ACCOUNT, 2, 2); assertEquals(7, mapper.item(ACCOUNT, 2).getDoneMask()); assertEquals(2, count("sr_english_vocabulary_review_log"));
    }

    @Test void concurrentModesAndSessionReplayDoNotLoseRatings() throws Exception {
        sql.commit(true);
        RatingRequest en = rating("EN_TO_ZH", "KNOW", "FREE", null), zh = rating("ZH_TO_EN", "FORGOT", "FREE", null);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            CountDownLatch gate = new CountDownLatch(1);
            List<Future<RatingResult>> futures = new ArrayList<>();
            for (RatingRequest request : List.of(en, zh)) futures.add(executor.submit(() -> rateConcurrent(gate, request)));
            gate.countDown(); for (Future<RatingResult> future : futures) assertFalse(future.get(20, TimeUnit.SECONDS).isDuplicate());
        }
        sql.clearCache(); sql.rollback(true); State state = service.states(ACCOUNT, List.of(1L)).getFirst(); assertEquals(2, state.getMemoryCount()); assertEquals(2, state.getModeMemory().size());
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            RatingRequest audio = rating("AUDIO_TO_BOTH", "KNOW", "FREE", null); CountDownLatch gate = new CountDownLatch(1);
            Future<RatingResult> one = executor.submit(() -> rateConcurrent(gate, audio)), two = executor.submit(() -> rateConcurrent(gate, audio)); gate.countDown();
            assertNotEquals(one.get(20, TimeUnit.SECONDS).isDuplicate(), two.get(20, TimeUnit.SECONDS).isDuplicate());
        }
        sql.clearCache(); sql.rollback(true); assertEquals(3, service.states(ACCOUNT, List.of(1L)).getFirst().getMemoryCount()); assertEquals(4, count("sr_english_vocabulary_review_log"));
    }
    private RatingResult rateConcurrent(CountDownLatch gate, RatingRequest request) throws Exception {
        try (SqlSession other = openSession()) {
            other.getConnection().setCatalog(schema); gate.await(10, TimeUnit.SECONDS);
            RatingResult result = service(other).rate(ACCOUNT, 1, request); other.commit(); return result;
        }
    }

    @Test void tenThousandWordsThirtyThousandDirectionsUseBoundedIndexedQueueAndSeek() throws Exception {
        addWords(46, 10000); confirmAll(); assertEquals(500, service.plan(ACCOUNT).getTotalGroups()); assertEquals(20, service.items(ACCOUNT, 1, 400, 0, 100).size());
        try (Statement statement = sql.getConnection().createStatement()) {
            statement.executeUpdate("INSERT INTO sr_english_vocabulary_mode_memory(account_id,word_id,direction,rating_count,last_rating,next_review_at) SELECT 42,w.id,d.direction,1,'KNOW','2026-10-01' FROM sr_english_vocabulary_word w CROSS JOIN (SELECT 'EN_TO_ZH' AS direction UNION ALL SELECT 'ZH_TO_EN' UNION ALL SELECT 'AUDIO_TO_BOTH') d");
            statement.execute("ANALYZE TABLE sr_english_vocabulary_mode_memory");
            try (ResultSet explain = statement.executeQuery("EXPLAIN SELECT word_id,direction,rating_count,ema_score,review_step,recent_good_count,last_rating,first_rated_at,last_rated_at,next_review_at,audio_verified FROM sr_english_vocabulary_mode_memory mm WHERE account_id=42 AND next_review_at<='2026-10-08' AND EXISTS(SELECT 1 FROM sr_english_vocabulary_word w WHERE w.id=mm.word_id) ORDER BY next_review_at,word_id,direction LIMIT 20")) {
                assertTrue(explain.next()); assertEquals("idx_sr_vocab_mode_due", explain.getString("key")); assertFalse(explain.getString("Extra").contains("filesort"));
            }
        }
        assertEquals(30000, service.reviewSummary(42).getDueCards()); assertEquals(10000, service.reviewSummary(42).getDueWords());
        var first = service.queue(42, 20, null); var next = service.queue(42, 20, first.getNextCursor()); assertEquals(20, first.getItems().size()); assertEquals(20, next.getItems().size());
        Set<String> keys = new HashSet<>(); for (var card : first.getItems()) keys.add(card.getWord().getId()+":"+card.getDirection());
        for (var card : next.getItems()) assertFalse(keys.contains(card.getWord().getId()+":"+card.getDirection()));
        assertEquals(20, service.queue(42, 20, null).getItems().size()); assertEquals(1, count("sr_english_vocabulary_review_log"));
    }
    private static class MutableClock extends Clock {
        private final AtomicReference<Instant> instant = new AtomicReference<>(Instant.parse("2026-10-08T00:00:00Z"));
        void at(String value) { instant.set(Instant.parse(value)); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant.get(); }
    }
}
