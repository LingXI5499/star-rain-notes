package com.starrainnotes.media.api;

/** Removes extracted prototype files when an ARCHIVE media asset is deleted. */
public interface PrototypeCleanupPort {
    void validateArchive(byte[] bytes);

    void cleanupExtractedByMediaAssetId(Long mediaAssetId);
}
