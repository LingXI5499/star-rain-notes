package com.starrainnotes.english.listening.media;

import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.media.api.MediaAssetApi;
import com.starrainnotes.media.api.MediaReferenceApi;
import com.starrainnotes.media.api.dto.MediaAssetSummary;
import com.starrainnotes.media.api.dto.MediaReferenceCommand;
import com.starrainnotes.media.api.constant.MediaUsageCodes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ListeningMediaReferences {
    private static final String MODULE = "ENGLISH";
    private static final String SOURCE_TYPE = "LISTENING_ITEM";
    private final MediaAssetApi assets;
    private final MediaReferenceApi references;

    public void replace(long itemId, Long audioMediaId) {
        if (audioMediaId != null) {
            assets.assertUsable(audioMediaId);
            MediaAssetSummary asset = assets.get(audioMediaId);
            if (!"AUDIO".equals(asset.getMediaType()) || !"PUBLIC".equals(asset.getAccessLevel()))
                throw new ApiException("ENGLISH_AUDIO_INVALID", "请选择公开的音频文件", 400);
        }
        references.detachAll(MODULE, SOURCE_TYPE, itemId);
        if (audioMediaId != null) {
            references.attach(MediaReferenceCommand.builder()
                    .mediaAssetId(audioMediaId)
                    .sourceModule(MODULE)
                    .sourceType(SOURCE_TYPE)
                    .sourceId(itemId)
                    .usageCode(MediaUsageCodes.ENGLISH_LISTENING_AUDIO)
                    .build());
        }
    }

    public void remove(long itemId) {
        references.detachAll(MODULE, SOURCE_TYPE, itemId);
    }
}
