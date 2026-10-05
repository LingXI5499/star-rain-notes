package com.starrainnotes.tutorial.content.media;

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

    public void replaceChapter(Long chapterId, String markdown) {
        mediaReferenceApi.detachAll(MODULE, CHAPTER, chapterId);
        attach(CHAPTER, chapterId, TutorialContentMediaParser.extractMediaAssetIds(markdown));
    }

    public void attachRevision(Long revisionId, Collection<String> chapterBodies) {
        LinkedHashSet<Long> assetIds = new LinkedHashSet<>();
        for (String body : chapterBodies) {
            assetIds.addAll(TutorialContentMediaParser.extractMediaAssetIds(body));
        }
        attach(REVISION, revisionId, assetIds);
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
