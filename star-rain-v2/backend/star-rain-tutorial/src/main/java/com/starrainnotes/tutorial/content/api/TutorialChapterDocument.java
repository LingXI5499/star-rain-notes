package com.starrainnotes.tutorial.content.api;

public record TutorialChapterDocument(Long id, String slug, String title, String summary,
                                      String bodyMarkdown, String searchableText) {
}
