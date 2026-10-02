package com.starrainnotes.media.service.impl;

import com.starrainnotes.media.api.MediaReferenceApi;
import com.starrainnotes.media.api.dto.MediaReferenceCommand;
import com.starrainnotes.media.api.dto.MediaReferenceView;
import com.starrainnotes.media.service.MediaReferenceService;
import com.starrainnotes.media.vo.MediaReferenceVO;
import java.util.List;
import org.springframework.stereotype.Component;

/*
 * 把模块内的 MediaReferenceService 转接为业务模块可用的 MediaReferenceApi。
 *
 * 只做视图转换与转发，不追加业务规则：
 * 所有校验与事务边界都留在 MediaReferenceServiceImpl，避免两处规则漂移。
 */
@Component
public class MediaReferenceApiAdapter implements MediaReferenceApi {

    private final MediaReferenceService referenceService;

    public MediaReferenceApiAdapter(MediaReferenceService referenceService) {
        this.referenceService = referenceService;
    }

    @Override
    public void attach(MediaReferenceCommand command) {
        referenceService.attach(command);
    }

    @Override
    public void detach(MediaReferenceCommand command) {
        referenceService.detach(command);
    }

    @Override
    public void detachAll(String sourceModule, String sourceType, Long sourceId) {
        referenceService.detachAll(sourceModule, sourceType, sourceId);
    }

    @Override
    public List<MediaReferenceView> listByAsset(Long mediaAssetId) {
        return referenceService.listByAsset(mediaAssetId).stream()
                .map(MediaReferenceApiAdapter::toView)
                .toList();
    }

    @Override
    public long countByAsset(Long mediaAssetId) {
        return referenceService.countByAsset(mediaAssetId);
    }

    private static MediaReferenceView toView(MediaReferenceVO vo) {
        return new MediaReferenceView(vo.getMediaAssetId(), vo.getSourceModule(), vo.getSourceType(),
                vo.getSourceId(), vo.getUsageCode());
    }
}
