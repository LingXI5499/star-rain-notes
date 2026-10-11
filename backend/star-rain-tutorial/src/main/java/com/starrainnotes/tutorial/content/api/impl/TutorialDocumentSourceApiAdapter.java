package com.starrainnotes.tutorial.content.api.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.tutorial.content.api.TutorialChapterDocument;
import com.starrainnotes.tutorial.content.api.TutorialDocumentSourceApi;
import com.starrainnotes.tutorial.content.api.TutorialPublishedDocument;
import com.starrainnotes.tutorial.content.exception.TutorialNotFoundException;
import com.starrainnotes.tutorial.content.service.TutorialPublicationService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TutorialDocumentSourceApiAdapter implements TutorialDocumentSourceApi {
    private final TutorialPublicationService publication;

    @Override
    public PageResult<TutorialPublishedDocument> page(int page, int pageSize) {
        PageResult<JsonNode> result = publication.publicTutorials(null, null, page, pageSize);
        return PageResult.<TutorialPublishedDocument>builder()
            .items(result.getItems().stream().map(row -> bySlug(row.path("slug").asText()).orElseThrow()).toList())
            .page(result.getPage()).pageSize(result.getPageSize()).total(result.getTotal()).build();
    }

    @Override
    public Optional<TutorialPublishedDocument> bySlug(String slug) {
        try {
            JsonNode detail = publication.publicTutorial(slug);
            List<TutorialChapterDocument> chapters = new ArrayList<>();
            for (JsonNode group : detail.path("groups")) {
                for (JsonNode item : group.path("chapters")) {
                    chapter(slug, item.path("slug").asText()).ifPresent(chapters::add);
                }
            }
            String published = detail.path("publishedAt").asText("");
            TutorialPublishedDocument document = new TutorialPublishedDocument(id(detail), detail.path("slug").asText(),
                detail.path("title").asText(), detail.path("summary").asText(),
                published.isBlank() ? null : LocalDateTime.parse(published), List.copyOf(chapters));
            LocalDateTime modified = modifiedAt(detail);
            for (var chapter : chapters) {
                if (chapter.getUpdatedAt() != null && (modified == null || chapter.getUpdatedAt().isAfter(modified))) modified = chapter.getUpdatedAt();
            }
            document.setUpdatedAt(modified);
            return Optional.of(document);
        } catch (TutorialNotFoundException absent) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<TutorialChapterDocument> chapter(String tutorialSlug, String chapterSlug) {
        try {
            JsonNode detail = publication.publicChapter(tutorialSlug, chapterSlug);
            StringBuilder searchable = new StringBuilder(detail.path("bodyMarkdown").asText());
            for (JsonNode card : detail.path("cards")) {
                searchable.append(' ').append(card.path("frontText").asText());
                searchable.append(' ').append(card.path("backMarkdown").asText());
            }
            for (JsonNode question : detail.path("questions")) {
                searchable.append(' ').append(question.path("questionText").asText());
            }
            TutorialChapterDocument document = new TutorialChapterDocument(id(detail), detail.path("slug").asText(),
                detail.path("title").asText(), detail.path("summary").asText(),
                detail.path("bodyMarkdown").asText(), searchable.toString());
            document.setUpdatedAt(modifiedAt(detail));
            return Optional.of(document);
        } catch (TutorialNotFoundException absent) {
            return Optional.empty();
        }
    }

    private Long id(JsonNode node) { return Long.valueOf(node.path("id").asText()); }

    private LocalDateTime modifiedAt(JsonNode publishedSnapshot) {
        String value = publishedSnapshot.path("updatedAt").asText("");
        if (value.isBlank()) return null;
        try { return LocalDateTime.parse(value); }
        catch (java.time.format.DateTimeParseException absentDate) { return null; }
    }
}
