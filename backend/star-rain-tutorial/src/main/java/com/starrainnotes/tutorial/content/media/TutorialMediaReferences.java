package com.starrainnotes.tutorial.content.media;

import com.starrainnotes.tutorial.content.mapper.TutorialQuestionMapper;
import com.starrainnotes.tutorial.content.mapper.TutorialKnowledgeCardMapper;
import com.starrainnotes.media.api.MediaReferenceApi;
import com.starrainnotes.media.api.dto.MediaReferenceCommand;
import com.starrainnotes.media.api.constant.MediaUsageCodes;
import com.starrainnotes.tutorial.content.utils.TutorialContentMediaParser;
import java.util.Collection;
import java.util.LinkedHashSet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TutorialMediaReferences {
    private static final String MODULE = "TUTORIAL";
    private static final String CHAPTER = "CHAPTER";
    private static final String REVISION = "REVISION";
    private final MediaReferenceApi mediaReferenceApi;
    private final TutorialKnowledgeCardMapper cardMapper;
    private final TutorialQuestionMapper questionMapper;

    public void replaceChapter(Long chapterId, String markdown) {
        mediaReferenceApi.detachAll(MODULE, CHAPTER, chapterId);
        LinkedHashSet<Long> assets = new LinkedHashSet<>(TutorialContentMediaParser.extractMediaAssetIds(markdown));
        cardMapper.listByChapterId(chapterId).forEach(card -> assets.addAll(TutorialContentMediaParser.extractMediaAssetIds(card.getBackMarkdown())));
        questionMapper.listByChapterId(chapterId).forEach(question -> assets.addAll(TutorialContentMediaParser.extractMediaAssetIds(question.getReferenceAnswer())));
        attach(CHAPTER, chapterId, assets);
    }

    public void attachRevision(Long revisionId, Collection<String> chapterBodies) {
        LinkedHashSet<Long> assetIds = new LinkedHashSet<>();
        for (String body : chapterBodies) {
            assetIds.addAll(TutorialContentMediaParser.extractMediaAssetIds(body));
        }
        attach(REVISION, revisionId, assetIds);
    }

    public void detachChapter(Long chapterId) {
        mediaReferenceApi.detachAll(MODULE, CHAPTER, chapterId);
    }

    public void detachRevision(Long revisionId) {
        mediaReferenceApi.detachAll(MODULE, REVISION, revisionId);
    }

    private void attach(String sourceType, Long sourceId, Collection<Long> assetIds) {
        for (Long mediaAssetId : assetIds) {
            mediaReferenceApi.attach(MediaReferenceCommand.builder()
                    .mediaAssetId(mediaAssetId)
                    .sourceModule(MODULE)
                    .sourceType(sourceType)
                    .sourceId(sourceId)
                    .usageCode(MediaUsageCodes.TUTORIAL_CHAPTER_CONTENT)
                    .build());
        }
    }
}
