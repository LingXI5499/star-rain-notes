package com.starrainnotes.english.reading.domain;

import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.reading.entity.ReadingArticle;
import com.starrainnotes.english.reading.infrastructure.ReadingRelationRepository;
import com.starrainnotes.media.api.MediaAssetPort;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReadingPublishPolicyTest {

    private final ReadingRelationRepository relations = mock(ReadingRelationRepository.class);
    private final MediaAssetPort media = mock(MediaAssetPort.class);
    private final ReadingPublishPolicy policy = new ReadingPublishPolicy(relations, media);

    @Test
    void preservesAllPublishViolationsAndTheirOrder() {
        ReadingArticle article = new ReadingArticle();
        article.setId(12L);
        article.setReadingLevel(4);
        article.setCoverMediaId(34L);
        assertThat(policy.violations(article)).isEqualTo(List.of(
                "标题不能为空", "slug 不能为空", "摘要不能为空", "正文不能为空",
                "能力层级必须为1/2/3", "CEFR 等级不能为空",
                "至少需要一个主题(TOPIC)标签", "至少需要一个文体(GENRE)标签", "封面必须为图片"));
    }

    @Test
    void acceptsTheExistingValidCombination() {
        ReadingArticle article = new ReadingArticle();
        article.setId(12L);
        article.setTitle("Title");
        article.setSlug("slug");
        article.setSummary("Summary");
        article.setBodyMarkdown("Body");
        article.setReadingLevel(2);
        article.setCefrLevel("B1");
        article.setCoverMediaId(34L);
        when(relations.hasEnabledDimension(12L, "TOPIC")).thenReturn(true);
        when(relations.hasEnabledDimension(12L, "GENRE")).thenReturn(true);
        when(media.isImage(34L)).thenReturn(true);
        assertThat(policy.violations(article)).isEmpty();
    }

    @Test
    void invalidLevelKeepsTheFrozenErrorCode() {
        assertThatThrownBy(() -> policy.requireLevel(4))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getCode())
                .isEqualTo("ENGLISH_READING_LEVEL_INVALID");
    }
}
