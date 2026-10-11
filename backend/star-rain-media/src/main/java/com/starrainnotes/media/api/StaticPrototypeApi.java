package com.starrainnotes.media.api;

/** Static ZIP capability: callers pass asset IDs and relative paths, never storage paths. */
public interface StaticPrototypeApi {
    void validate(Long assetId, String entryPath);
    byte[] read(Long assetId, String path);
    String contentType(String path);
}
