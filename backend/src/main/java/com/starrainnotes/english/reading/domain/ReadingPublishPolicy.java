package com.starrainnotes.english.reading.domain;

import com.starrainnotes.english.reading.entity.ReadingArticle;
import com.starrainnotes.english.reading.infrastructure.ReadingRelationRepository;
import com.starrainnotes.english.api.MediaPort;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

/** Preserves the original publish violations and their order. */
@Component
public class ReadingPublishPolicy {
    private final ReadingRelationRepository relations;
    private final MediaPort media;
    public ReadingPublishPolicy(ReadingRelationRepository relations, MediaPort media) {
        this.relations = relations;
        this.media = media;
    }
    public List<String> violations(ReadingArticle article) {
        List<String> problems = new ArrayList<>();
        if (blank(article.getTitle())) problems.add("标题不能为空");
        if (blank(article.getSlug())) problems.add("slug 不能为空");
        if (blank(article.getSummary())) problems.add("摘要不能为空");
        if (blank(article.getBodyMarkdown())) problems.add("正文不能为空");
        if (article.getReadingLevel() == null || article.getReadingLevel() < 1 || article.getReadingLevel() > 3) {
            problems.add("能力层级必须为1/2/3");
        }
        if (blank(article.getCefrLevel())) problems.add("CEFR 等级不能为空");
        Long id = article.getId() == null ? 0L : article.getId();
        if (!relations.hasEnabledDimension(id, "TOPIC")) problems.add("至少需要一个主题(TOPIC)标签");
        if (!relations.hasEnabledDimension(id, "GENRE")) problems.add("至少需要一个文体(GENRE)标签");
        if (article.getCoverMediaId() != null && !media.isImage(article.getCoverMediaId())) {
            problems.add("封面必须为图片");
        }
        return problems;
    }
    public void requireLevel(Integer level) {
        if (level == null || level < 1 || level > 3) {
            throw new com.starrainnotes.common.error.ApiException(
                    org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_READING_LEVEL_INVALID",
                    "Invalid reading level", "Reading level must be 1, 2 or 3.");
        }
    }
    private boolean blank(String value) { return value == null || value.isBlank(); }
}
