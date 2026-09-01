package com.starrainnotes.tutorial.dto;

import java.util.List;

/**
 * Public chapter reader payload (04 §10): body markdown, tutorial context,
 * breadcrumbs and Prev/Next. The sidebar tree and the TOC are NOT returned —
 * the sidebar comes from the tutorial detail endpoint and the TOC is derived
 * client-side from the rendered markdown headings.
 */
public record PublicChapterView(
        Long chapterId,
        String chapterSlug,
        String chapterTitle,
        Long tutorialId,
        String tutorialSlug,
        String tutorialTitle,
        String tutorialSummary,
        String bodyMarkdown,
        String summary,
        List<BreadcrumbView> breadcrumbs,
        PrevNextView previous,
        PrevNextView next,
        String publishedAt,
        String updatedAt) {
}
