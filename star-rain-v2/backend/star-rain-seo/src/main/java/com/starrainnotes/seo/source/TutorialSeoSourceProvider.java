package com.starrainnotes.seo.source;

import com.fasterxml.jackson.databind.JsonNode;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.tutorial.content.exception.TutorialNotFoundException;
import com.starrainnotes.tutorial.content.service.TutorialPublicationService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TutorialSeoSourceProvider implements SeoSourceProvider {
    private final TutorialPublicationService tutorials;

    @Override
    public boolean supports(String path) {
        return path != null && path.matches("/tutorials/[a-zA-Z0-9_-]{1,120}(/[a-zA-Z0-9_-]{1,120})?");
    }

    @Override
    public Optional<SeoSourceDocument> loadByRoute(String path) {
        if (!supports(path)) return Optional.empty();
        String[] parts = path.split("/");
        try {
            if (parts.length == 3) return Optional.of(tutorial(tutorials.publicTutorial(parts[2])));
            JsonNode detail = tutorials.publicChapter(parts[2], parts[3]);
            return Optional.of(chapter(parts[2], detail));
        } catch (TutorialNotFoundException absent) { return Optional.empty(); }
    }

    @Override
    public List<SeoSourceDocument> listPublished() {
        List<SeoSourceDocument> result = new ArrayList<>();
        for (int page = 1; ; page++) {
            PageResult<JsonNode> rows = tutorials.publicTutorials(null, null, page, 100);
            for (JsonNode summary : rows.getItems()) {
                String slug = summary.path("slug").asText();
                result.addAll(publishedTree(slug));
            }
            if ((long) page * 100 >= rows.getTotal()) break;
        }
        return result;
    }

    public List<SeoSourceDocument> publishedTree(String slug) {
        JsonNode detail = tutorials.publicTutorial(slug);
        List<SeoSourceDocument> result = new ArrayList<>();
        result.add(tutorial(detail));
        for (JsonNode group : detail.path("groups")) {
            for (JsonNode chapter : group.path("chapters")) {
                result.add(chapter(slug, tutorials.publicChapter(slug, chapter.path("slug").asText())));
            }
        }
        return result;
    }

    private SeoSourceDocument tutorial(JsonNode node) {
        StringBuilder body = new StringBuilder(node.path("summary").asText());
        for (JsonNode group : node.path("groups")) {
            body.append('\n').append(group.path("title").asText());
            for (JsonNode chapter : group.path("chapters")) {
                body.append('\n').append(chapter.path("title").asText());
                body.append(' ').append(chapter.path("summary").asText());
            }
        }
        return SeoSourceDocument.builder().routePath("/tutorials/" + node.path("slug").asText())
            .contentType("TUTORIAL").contentId(Long.valueOf(node.path("id").asText()))
            .title(node.path("title").asText()).summary(node.path("summary").asText())
            .bodyMarkdown(body.toString()).build();
    }

    private SeoSourceDocument chapter(String tutorialSlug, JsonNode node) {
        StringBuilder text = new StringBuilder(node.path("bodyMarkdown").asText());
        for (JsonNode card : node.path("cards")) {
            text.append(' ').append(card.path("frontText").asText());
            text.append(' ').append(card.path("backMarkdown").asText());
        }
        for (JsonNode question : node.path("questions")) text.append(' ').append(question.path("questionText").asText());
        return SeoSourceDocument.builder().routePath("/tutorials/" + tutorialSlug + "/" + node.path("slug").asText())
            .contentType("CHAPTER").contentId(Long.valueOf(node.path("id").asText()))
            .title(node.path("title").asText()).summary(node.path("summary").asText())
            .bodyMarkdown(text.toString()).build();
    }
}
