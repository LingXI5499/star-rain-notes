package com.starrainnotes.english.listening.application;

import com.starrainnotes.english.listening.dto.PronunciationRuleView;
import com.starrainnotes.english.listening.infrastructure.PronunciationRuleRepository;
import org.springframework.stereotype.Service;
import java.util.List;

/** Read use cases for pronunciation rules. */
@Service
public class PronunciationRuleQueryService {
    private final PronunciationRuleRepository repository;
    public PronunciationRuleQueryService(PronunciationRuleRepository repository) { this.repository = repository; }
    public List<PronunciationRuleView> list(boolean publishedOnly) { return repository.pronunciationRules(publishedOnly); }
    public PronunciationRuleView get(Long id) { return repository.ruleById(id, false); }
    public PronunciationRuleView publicGet(String slug) { return repository.publicRule(slug); }
}
