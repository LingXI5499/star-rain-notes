package com.starrainnotes.media.service;

import com.starrainnotes.media.vo.MediaAssetVO;

/** Public image preparation and registration for content editors. */
public interface EditorImageService {
    MediaAssetVO prepare(Long id);
    MediaAssetVO importUrl(String url);
}
