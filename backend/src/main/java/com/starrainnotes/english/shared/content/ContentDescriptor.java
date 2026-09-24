package com.starrainnotes.english.shared.content;

public record ContentDescriptor(EnglishContentType type, long id, String slug, String title,
                                String summary, String cefrLevel, String coverUrl,
                                String publishStatus, int sortOrder) {
    public boolean published() { return "PUBLISHED".equals(publishStatus); }
    public ContentDescriptor withCoverUrl(String url) {
        return new ContentDescriptor(type, id, slug, title, summary, cefrLevel, url,
                publishStatus, sortOrder);
    }
}
