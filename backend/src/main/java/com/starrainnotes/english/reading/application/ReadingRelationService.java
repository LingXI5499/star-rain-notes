package com.starrainnotes.english.reading.application;

import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.reading.dto.ReadingArticleRequest;
import com.starrainnotes.english.reading.infrastructure.ReadingRelationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.List;

/** Validates and replaces the tag/grammar relations in the article transaction. */
@Service
public class ReadingRelationService {
    private final ReadingRelationRepository repository;
    public ReadingRelationService(ReadingRelationRepository repository) { this.repository = repository; }

    public void replaceRelations(Long articleId, ReadingArticleRequest request) {
        repository.clear(articleId);
        boolean primaryAssigned = false;
        for (Long termId : nonNull(request.topicTagIds())) {
            requireEnabledTag(termId, "TOPIC");
            repository.addTag(articleId, termId, primaryAssigned ? "TAG" : "PRIMARY");
            primaryAssigned = true;
        }
        for (Long termId : nonNull(request.genreTagIds())) {
            requireEnabledTag(termId, "GENRE");
            repository.addTag(articleId, termId, "TAG");
        }
        for (Long termId : nonNull(request.abilityTagIds())) {
            requireEnabledTag(termId, "ABILITY");
            repository.addTag(articleId, termId, "TAG");
        }
        for (Long lessonId : nonNull(request.grammarLessonIds())) {
            requireGrammarLesson(lessonId);
            repository.addGrammarLesson(articleId, lessonId);
        }
    }

    private void requireEnabledTag(Long termId, String expectedDimension) {
        List<String> found = repository.enabledTagDimensions(termId);
        if (found.isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_TAXONOMY_NOT_FOUND",
                    "Invalid taxonomy term", "The selected tag does not exist or is disabled.");
        }
        if (!expectedDimension.equals(found.get(0))) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_TAXONOMY_DEPTH_INVALID",
                    "Invalid tag dimension",
                    "A " + expectedDimension + " tag cannot reference the '" + found.get(0) + "' dimension.");
        }
    }
    private List<Long> nonNull(List<Long> ids) {
        if (ids == null) return List.of();
        return ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
    }
    private void requireGrammarLesson(Long lessonId) {
        if (!repository.grammarLessonExists(lessonId)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_READING_GRAMMAR_LESSON_INVALID",
                    "Invalid grammar lesson", "The selected grammar lesson does not exist.");
        }
    }
}
