package com.starrainnotes.tutorial.content.api.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.starrainnotes.tutorial.content.api.PublishedTutorial;
import com.starrainnotes.tutorial.content.api.TutorialReferenceApi;
import com.starrainnotes.tutorial.content.entity.TutorialEntity;
import com.starrainnotes.tutorial.content.mapper.TutorialMapper;
import com.starrainnotes.tutorial.content.service.TutorialPublicationService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TutorialReferenceApiImpl implements TutorialReferenceApi {
    private final TutorialMapper tutorials;
    private final TutorialPublicationService publication;

    @Override
    @Transactional(readOnly = true)
    public boolean exists(Long tutorialId) {
        return tutorialId != null && tutorials.selectById(tutorialId) != null;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PublishedTutorial> publishedTutorial(Long tutorialId) {
        TutorialEntity row = tutorialId == null ? null : tutorials.selectById(tutorialId);
        if (row == null || !"PUBLISHED".equals(row.getPublicationStatus())
                || row.getPublishedRevisionId() == null) {
            return Optional.empty();
        }
        JsonNode snapshot = publication.publicTutorial(row.getSlug());
        return Optional.of(new PublishedTutorial(row.getId(), row.getSlug(), snapshot.path("title").asText(),
                snapshot.path("summary").asText(), null, row.getPublishedAt()));
    }
}
