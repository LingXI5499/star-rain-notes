package com.starrainnotes.seo.service;

import com.starrainnotes.seo.dto.SeoSourceDocument;

import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.tutorial.content.api.TutorialChapterDocument;
import com.starrainnotes.tutorial.content.api.TutorialDocumentSourceApi;
import com.starrainnotes.tutorial.content.api.TutorialPublishedDocument;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TutorialSeoSourceProvider implements SeoSourceProvider {
    private final TutorialDocumentSourceApi tutorials;

    @Override
    public boolean supports(String path) {
        return path != null && path.matches("/tutorials/[a-zA-Z0-9_-]{1,120}(/[a-zA-Z0-9_-]{1,120})?");
    }

    @Override
    public Optional<SeoSourceDocument> loadByRoute(String path) {
        if (!supports(path)) return Optional.empty();
        String[] parts = path.split("/");
        if (parts.length == 3) return tutorials.bySlug(parts[2]).map(this::tutorial);
        return tutorials.chapter(parts[2], parts[3]).map(chapter -> chapter(parts[2], chapter));
    }

    @Override
    public List<SeoSourceDocument> listPublished() {
        List<SeoSourceDocument> result = new ArrayList<>();
        for (int page = 1; ; page++) {
            PageResult<TutorialPublishedDocument> rows = tutorials.page(page, 100);
            for (TutorialPublishedDocument item : rows.getItems()) result.addAll(publishedTree(item));
            if ((long) page * 100 >= rows.getTotal()) break;
        }
        return result;
    }

    public List<SeoSourceDocument> publishedTree(String slug) {
        return tutorials.bySlug(slug).map(this::publishedTree).orElseGet(List::of);
    }

    private List<SeoSourceDocument> publishedTree(TutorialPublishedDocument detail) {
        List<SeoSourceDocument> result = new ArrayList<>();
        result.add(tutorial(detail));
        detail.chapters().forEach(chapter -> result.add(chapter(detail.slug(), chapter)));
        return result;
    }

    private SeoSourceDocument tutorial(TutorialPublishedDocument detail) {
        StringBuilder body = new StringBuilder(detail.summary() == null ? "" : detail.summary());
        detail.chapters().forEach(chapter -> body.append('\n').append(chapter.title())
            .append(' ').append(chapter.summary()));
        return SeoSourceDocument.builder().routePath("/tutorials/" + detail.slug())
            .contentType("TUTORIAL").contentId(detail.id()).title(detail.title())
            .summary(detail.summary()).bodyMarkdown(body.toString()).build();
    }

    private SeoSourceDocument chapter(String tutorialSlug, TutorialChapterDocument detail) {
        return SeoSourceDocument.builder().routePath("/tutorials/" + tutorialSlug + "/" + detail.slug())
            .contentType("CHAPTER").contentId(detail.id()).title(detail.title())
            .summary(detail.summary()).bodyMarkdown(detail.searchableText()).build();
    }
}
