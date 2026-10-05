package com.starrainnotes.english.vocabulary.service.impl;

import com.starrainnotes.english.vocabulary.constant.VocabularyStudyConstants;
import com.starrainnotes.english.vocabulary.dto.VocabularyLocalProgressRequest;
import com.starrainnotes.english.vocabulary.enumeration.LearningStatus;
import com.starrainnotes.english.vocabulary.enumeration.ReviewDirection;
import com.starrainnotes.english.vocabulary.enumeration.TimingStatus;
import com.starrainnotes.english.vocabulary.mapper.VocabularyStudyMapper;
import com.starrainnotes.english.vocabulary.service.VocabularyProgressImportService;
import com.starrainnotes.english.vocabulary.utils.VocabularyReviewPolicy;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * 本机进度导入实现。
 *
 * 容错原则（与 V1 一致）：客户端数据不可信，坏行只丢弃、不整批失败——
 * 用户的记忆进度是长期积累的数据，不能因为一行脏数据就让整个导入报错。
 * 只增不减：合并语句里计数取较大值、时间取更晚的一个。
 */
@Service
@RequiredArgsConstructor
public class VocabularyProgressImportServiceImpl implements VocabularyProgressImportService {

    private static final List<String> KNOWN_TIMING = List.of(
            TimingStatus.NEW.name(), TimingStatus.EARLY.name(),
            TimingStatus.ON_TIME.name(), TimingStatus.OVERDUE.name());

    private final VocabularyStudyMapper mapper;

    @Override
    @Transactional
    public void importLocal(long accountId, VocabularyLocalProgressRequest request) {
        if (request == null) {
            return;
        }
        LocalDateTime now = utcNow();
        importMemoryList(accountId, request.getMemory(), now);
        importLegacyVocabulary(accountId, request.getVocabulary(), now);
        importReviewLog(accountId, request.getReviewLog(), now);
    }

    private void importMemoryList(long accountId, List<VocabularyLocalProgressRequest.LocalMemoryPayload> entries,
                                  LocalDateTime now) {
        if (entries == null) {
            return;
        }
        for (VocabularyLocalProgressRequest.LocalMemoryPayload entry : entries) {
            if (entry == null || entry.getWordId() == null) {
                continue;
            }
            importMemory(accountId, entry.getWordId(), entry, now);
        }
    }

    /*
     * V1 更早的本机结构是 { wordId: { memoryCount, lastMemoryAt } }，
     * 迁移到结构化存储之前留下的数据仍然要能导进来。
     */
    private void importLegacyVocabulary(long accountId, Map<String, VocabularyLocalProgressRequest.LocalMemoryPayload> entries,
                                        LocalDateTime now) {
        if (entries == null) {
            return;
        }
        for (Map.Entry<String, VocabularyLocalProgressRequest.LocalMemoryPayload> entry : entries.entrySet()) {
            Long wordId = longValue(entry.getKey());
            if (wordId == null || entry.getValue() == null) {
                continue;
            }
            importMemory(accountId, wordId, entry.getValue(), now);
        }
    }

    private void importMemory(long accountId, long wordId,
                              VocabularyLocalProgressRequest.LocalMemoryPayload payload, LocalDateTime now) {
        if (wordId <= 0 || mapper.wordExists(wordId) == 0) {
            return;
        }
        int memoryCount = Math.max(0, intValue(payload.getMemoryCount()));
        int reviewCount = Math.max(0, intValue(payload.getReviewCount()));
        boolean active = LearningStatus.ACTIVE.name().equals(payload.getLearningStatus());
        /*
         * 只导入真的有进度的行：有记忆/复习次数，或者已经加入计划（ACTIVE 但还没复习）。
         * 「加入计划但零次记忆」也必须导入——游客把整个主题加进计划后登录合并，
         * 计划本身就是要带过来的进度；只有 NEW 且零计数的行没有导入意义。
         * （这一条比 V1 宽：V1 的导入只认 memoryCount/reviewCount > 0，会把纯计划成员丢掉。）
         */
        if (memoryCount == 0 && reviewCount == 0 && !active) {
            return;
        }
        int reviewStep = Math.max(0, Math.min(VocabularyStudyConstants.MAX_REVIEW_STEP,
                payload.getReviewStep() == null ? reviewCount : intValue(payload.getReviewStep())));
        LocalDateTime firstLearnedAt = parseTime(payload.getFirstLearnedAt(), now);
        LocalDateTime lastReviewedAt = parseTime(payload.getLastReviewedAt(), now);
        LocalDateTime nextReviewAt = parseTime(payload.getNextReviewAt(), now);
        LocalDateTime lastMemoryAt = parseTime(payload.getLastMemoryAt(), now);
        if (lastMemoryAt == null) {
            lastMemoryAt = lastReviewedAt;
        }
        mapper.importMemory(accountId, wordId, memoryCount, reviewStep, reviewCount,
                firstLearnedAt == null ? now : firstLearnedAt, lastReviewedAt,
                nextReviewAt == null ? now : nextReviewAt, lastMemoryAt, now);
    }

    private void importReviewLog(long accountId,
                                 List<VocabularyLocalProgressRequest.LocalReviewPayload> entries,
                                 LocalDateTime now) {
        if (entries == null) {
            return;
        }
        for (VocabularyLocalProgressRequest.LocalReviewPayload entry : entries) {
            if (entry == null || entry.getWordId() == null) {
                continue;
            }
            long wordId = entry.getWordId();
            if (wordId <= 0 || mapper.wordExists(wordId) == 0) {
                continue;
            }
            if (!ReviewDirection.isConcrete(entry.getDirection())) {
                continue;
            }
            if (!isUuid(entry.getReviewSessionId())) {
                continue;
            }
            String timing = KNOWN_TIMING.contains(entry.getTimingStatus())
                    ? entry.getTimingStatus()
                    : TimingStatus.NEW.name();
            LocalDateTime reviewedAt = parseTime(entry.getReviewedAt(), now);
            if (reviewedAt == null) {
                reviewedAt = now;
            }
            /* 客户端时钟漂移超过容差就按服务端时间记，避免出现未来的复习记录 */
            if (reviewedAt.isAfter(now.plusMinutes(VocabularyStudyConstants.IMPORT_FUTURE_TOLERANCE_MINUTES))) {
                reviewedAt = now;
            }
            int reviewNumber = Math.max(1, intValue(entry.getReviewNumber()));
            long interval = entry.getIntervalSeconds() == null
                    ? VocabularyReviewPolicy.intervalSeconds(reviewNumber)
                    : Math.max(1L, entry.getIntervalSeconds());
            mapper.importReview(accountId, wordId, entry.getReviewSessionId(), entry.getDirection(),
                    reviewNumber, parseTime(entry.getScheduledAt(), now), reviewedAt, interval, timing);
        }
    }

    private boolean isUuid(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    /*
     * 客户端给的是带偏移量的 ISO 串；解析失败或超出未来容差都返回 null，由调用方决定兜底。
     */
    private LocalDateTime parseTime(String value, LocalDateTime now) {
        if (value == null || value.isBlank() || "null".equals(value)) {
            return null;
        }
        try {
            LocalDateTime parsed = OffsetDateTime.parse(value)
                    .withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
            return parsed.isAfter(now.plusMinutes(VocabularyStudyConstants.IMPORT_FUTURE_TOLERANCE_MINUTES))
                    ? null
                    : parsed;
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private int intValue(Integer value) {
        return value == null ? 0 : value;
    }

    private Long longValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private LocalDateTime utcNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
