package com.starrainnotes.english.reading.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Admin/public reading-article detail (full body + stats + tag refs + grammar
 * refs + prev/next navigation). Nav is only populated on the public detail.
 */
public record ReadingArticleView(
        Long id,
        String title,
        String slug,
        String summary,
        String bodyMarkdown,
        Long coverMediaId,
        String coverUrl,
        Integer readingLevel,
        String cefrLevel,
        String sourceName,
        String sourceUrl,
        String copyrightNote,
        Integer wordCount,
        Integer uniqueWordCount,
        BigDecimal averageSentenceWords,
        Integer maxSentenceWords,
        Integer estimatedMinutes,
        String publishStatus,
        Integer sortOrder,
        String publishedAt,
        String updatedAt,
        List<ReadingTagRef> tags,
        List<ReadingGrammarRef> grammarLessons,
        ReadingArticleLinkView previous,
        ReadingArticleLinkView next) {

    public ReadingArticleView withNavigation(ReadingArticleLinkView prev, ReadingArticleLinkView nxt) {
        return new ReadingArticleView(id, title, slug, summary, bodyMarkdown, coverMediaId, coverUrl,
                readingLevel, cefrLevel, sourceName, sourceUrl, copyrightNote, wordCount, uniqueWordCount,
                averageSentenceWords, maxSentenceWords, estimatedMinutes, publishStatus, sortOrder,
                publishedAt, updatedAt, tags, grammarLessons, prev, nxt);
    }
}
