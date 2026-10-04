package com.starrainnotes.tutorial.content.api.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.starrainnotes.tutorial.content.api.PublishedTutorial;
import com.starrainnotes.tutorial.content.api.TutorialPublicApi;
import com.starrainnotes.tutorial.content.service.TutorialPublicationService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TutorialPublicApiImpl implements TutorialPublicApi {
    private final TutorialPublicationService publication;

    @Override
    public List<PublishedTutorial> latestPublished(int limit) {
        int size = Math.max(1, Math.min(limit, 12));
        return publication.publicTutorials(null, null, 1, size).getItems().stream()
                .map(this::summary).toList();
    }

    private PublishedTutorial summary(JsonNode item) {
        String published = item.path("publishedAt").asText("");
        return new PublishedTutorial(item.path("id").asLong(), item.path("slug").asText(),
                item.path("title").asText(), item.path("summary").asText(),
                item.path("coverUrl").asText(null),
                published.isBlank() ? null : LocalDateTime.parse(published));
    }
}
