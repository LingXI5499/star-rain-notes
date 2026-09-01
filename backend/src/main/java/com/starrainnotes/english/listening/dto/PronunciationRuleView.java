package com.starrainnotes.english.listening.dto;

public record PronunciationRuleView(
        Long id,
        String ruleType,
        String title,
        String slug,
        String summary,
        String bodyMarkdown,
        Long audioMediaId,
        String audioUrl,
        String publishStatus,
        Integer sortOrder,
        String publishedAt,
        String updatedAt,
        ListeningLinkView previous,
        ListeningLinkView next) {

    public PronunciationRuleView withNav(ListeningLinkView prev, ListeningLinkView nxt) {
        return new PronunciationRuleView(id, ruleType, title, slug, summary, bodyMarkdown, audioMediaId,
                audioUrl, publishStatus, sortOrder, publishedAt, updatedAt, prev, nxt);
    }
}
