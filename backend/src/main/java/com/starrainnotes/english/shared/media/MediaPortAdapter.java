package com.starrainnotes.english.shared.media;

import com.starrainnotes.english.api.MediaPort;
import com.starrainnotes.media.api.MediaAssetPort;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

/** Delegates English media capabilities to the Media module API. */
@Component
public class MediaPortAdapter implements MediaPort {
    private final MediaAssetPort media;

    public MediaPortAdapter(MediaAssetPort media) {
        this.media = media;
    }

    @Override public boolean isImage(Long id) { return media.isImage(id); }
    @Override public boolean isType(Long id, String type) { return media.isType(id, type); }
    @Override public String assetType(Long id) { return media.assetType(id); }
    @Override public String publicUrl(Long id) { return media.publicUrl(id); }
    @Override public Map<Long, String> publicUrls(Collection<Long> ids) { return media.publicUrls(ids); }
    @Override public void requireImageIfPresent(Long id) { media.requireImageIfPresent(id); }
}
