package com.starrainnotes.media.service;

import com.starrainnotes.media.api.dto.MediaReferenceCommand;
import com.starrainnotes.media.vo.MediaReferenceVO;
import java.util.List;

// 媒体引用的模块内服务契约。对业务模块暴露的入口是 api 包里的 MediaReferenceApi
public interface MediaReferenceService {

    void attach(MediaReferenceCommand command);

    void detach(MediaReferenceCommand command);

    void detachAll(String sourceModule, String sourceType, Long sourceId);

    List<MediaReferenceVO> listByAsset(Long mediaAssetId);

    long countByAsset(Long mediaAssetId);
}
