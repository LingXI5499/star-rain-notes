package com.starrainnotes.english.listening.application;

import com.starrainnotes.english.listening.dto.PronunciationRuleRequest;
import com.starrainnotes.english.listening.dto.PronunciationRuleView;
import com.starrainnotes.english.listening.infrastructure.PronunciationRuleRepository;
import com.starrainnotes.english.shared.events.EnglishContentChange;
import com.starrainnotes.english.shared.events.EnglishContentChangeType;
import com.starrainnotes.english.shared.events.EnglishContentKind;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Transactional use cases for pronunciation rule changes. */
@Service
public class PronunciationRuleCommandService {
    private final PronunciationRuleRepository repository;
    public PronunciationRuleCommandService(PronunciationRuleRepository repository) { this.repository = repository; }
    @Transactional public PronunciationRuleView create(PronunciationRuleRequest request) {
        return repository.createRule(request);
    }
    @Transactional
    @EnglishContentChange(kind = EnglishContentKind.PRONUNCIATION_RULE, changeType = EnglishContentChangeType.UPDATED)
    public PronunciationRuleView update(Long id, PronunciationRuleRequest request) {
        return repository.updateRule(id, request);
    }
    @Transactional public void delete(Long id) { repository.deleteRule(id); }
    @Transactional
    @EnglishContentChange(kind = EnglishContentKind.PRONUNCIATION_RULE, changeType = EnglishContentChangeType.PUBLISHED)
    public PronunciationRuleView publish(Long id) { return repository.publishRule(id); }
    @Transactional
    @EnglishContentChange(kind = EnglishContentKind.PRONUNCIATION_RULE, changeType = EnglishContentChangeType.WITHDRAWN)
    public PronunciationRuleView withdraw(Long id) { return repository.withdrawRule(id); }
    @Transactional public void move(Long id, int targetIndex) { repository.moveRule(id, targetIndex); }
}
