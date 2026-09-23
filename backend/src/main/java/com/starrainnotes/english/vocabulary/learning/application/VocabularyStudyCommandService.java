package com.starrainnotes.english.vocabulary.learning.application;

import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.vocabulary.application.VocabularyQueryService;
import com.starrainnotes.english.vocabulary.learning.*;
import com.starrainnotes.english.vocabulary.learning.domain.ReviewSchedule;
import com.starrainnotes.english.vocabulary.learning.domain.VocabularyReviewPolicy;
import com.starrainnotes.english.vocabulary.learning.infrastructure.VocabularyReviewLogRepository;
import com.starrainnotes.english.vocabulary.learning.infrastructure.VocabularyStudyRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class VocabularyStudyCommandService {
    private final VocabularyQueryService vocabulary;
    private final VocabularyStudyQueryService query;
    private final VocabularyStudyRepository studyRepository;
    private final VocabularyReviewLogRepository reviewLogRepository;
    private final VocabularyReviewPolicy reviewPolicy = new VocabularyReviewPolicy();

    public VocabularyStudyCommandService(VocabularyQueryService vocabulary,
                                         VocabularyStudyQueryService query, VocabularyStudyRepository studyRepository,
                                         VocabularyReviewLogRepository reviewLogRepository) {
        this.vocabulary=vocabulary;
        this.query=query;
        this.studyRepository=studyRepository;
        this.reviewLogRepository=reviewLogRepository;
    }

    @Transactional
    public void putMemoryCount(long accountId,long wordId,int memoryCount) {
        if(memoryCount<0) throw new ApiException(HttpStatus.NOT_FOUND,"INVALID_MEMORY_COUNT","Not found",
                "The requested data does not exist.");
        studyRepository.putMemoryCount(accountId,wordId,memoryCount);
    }
    @Transactional
    public VocabularyStudySettingsView updateSettings(long accountId, VocabularyStudySettingsRequest request) {
        if (!request.showEnglish() && !request.showChinese()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "VOCABULARY_LANGUAGE_REQUIRED",
                    "A language must remain visible", "English and Chinese cannot both be hidden.");
        }
        studyRepository.updateSettings(accountId, request);
        return query.settings(accountId);
    }


    @Transactional
    public VocabularyMemoryView start(long accountId, long wordId) {
        vocabulary.getWord(wordId);
        LocalDateTime now = utcNow();
        studyRepository.start(accountId, wordId, now);
        return query.memory(accountId, wordId);
    }


    @Transactional
    public VocabularyReviewResultView completeReview(long accountId, long wordId, VocabularyReviewRequest request) {
        vocabulary.getWord(wordId);
        var duplicate = reviewLogRepository.findBySession(accountId, request.reviewSessionId());
        if (duplicate.isPresent()) {
            VocabularyReviewLogRepository.ReviewLogResult row = duplicate.get();
            requireSameReviewWord(wordId, row);
            return new VocabularyReviewResultView(query.memory(accountId, wordId), row.reviewNumber(),
                    row.intervalSeconds(), row.timingStatus(), true);
        }

        LocalDateTime now = utcNow();
        var locked = studyRepository.lockMemory(accountId, wordId);
        if (locked.isEmpty()) {
            studyRepository.initializeForReview(accountId, wordId, now);
            locked = studyRepository.lockMemory(accountId, wordId);
        }
        VocabularyStudyRepository.MemoryLock old = locked.orElseThrow();
        ReviewSchedule schedule = reviewPolicy.next(old.reviewCount(), old.nextReviewAt(), now);

        boolean inserted = reviewLogRepository.insert(accountId, wordId, request.reviewSessionId(), request.direction(), schedule);
        if (!inserted) {
            VocabularyReviewLogRepository.ReviewLogResult row = reviewLogRepository.findBySession(accountId, request.reviewSessionId()).orElseThrow();
            requireSameReviewWord(wordId, row);
            return new VocabularyReviewResultView(query.memory(accountId, wordId), row.reviewNumber(),
                    row.intervalSeconds(), row.timingStatus(), true);
        }
        studyRepository.advance(accountId, wordId, schedule);
        return new VocabularyReviewResultView(query.memory(accountId, wordId), schedule.reviewNumber(),
                schedule.intervalSeconds(), schedule.timingStatus(), false);
    }


    @Transactional
    public void reset(long accountId, long wordId) {
        vocabulary.getWord(wordId);
        studyRepository.reset(accountId, wordId);
    }

    @Transactional
    public VocabularyMemoryView setDisplay(long accountId, long wordId, VocabularyDisplayRequest request) {
        vocabulary.getWord(wordId);
        studyRepository.setDisplay(accountId, wordId, request.displayMode());
        return query.optionalMemory(accountId, wordId, request.displayMode());
    }

    @Transactional
    public void clearDisplay(long accountId, long wordId) {
        studyRepository.clearDisplay(accountId, wordId);
    }


    private void requireSameReviewWord(long wordId, VocabularyReviewLogRepository.ReviewLogResult row) {
        if (row.wordId() != wordId) {
            throw new ApiException(HttpStatus.CONFLICT, "VOCABULARY_REVIEW_SESSION_REUSED",
                    "Review session already used", "The review session id belongs to another word.");
        }
    }

    private LocalDateTime utcNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
