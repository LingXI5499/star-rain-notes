package com.starrainnotes.media.service.impl;

import com.starrainnotes.media.api.MediaReferenceApi;
import com.starrainnotes.media.api.dto.MediaReferenceCommand;
import com.starrainnotes.media.api.vo.MediaReferenceVO;
import com.starrainnotes.media.service.MediaReferenceService;
import java.util.List;
import org.springframework.stereotype.Component;

/*
 * 把模块内的 MediaReferenceService 转接为业务模块可用的 MediaReferenceApi。
 *
 * 只做视图转换与转发，不追加业务规则：
 * 所有校验与事务边界都留在 MediaReferenceServiceImpl，避免两处规则漂移。
 *
 * 注意这里的同名类型：本类签名面对的是**契约** media.api.vo.MediaReferenceVO（已 import），
 * 而 referenceService.listByAsset(...) 返回的是**模块内** media.vo.MediaReferenceVO。
 * 两者字段不同（模块内那个多 id / createdAt 且做 Long→String 序列化），不是重复类型，
 * 因此下面引用模块内那个时必须写全限定名，不能再加同名的 import。
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
    public List<MediaReferenceVO> listByAsset(Long mediaAssetId) {
        return referenceService.listByAsset(mediaAssetId).stream()
                .map(MediaReferenceApiAdapter::toView)
                .toList();
    }

    @Override
    public long countByAsset(Long mediaAssetId) {
        return referenceService.countByAsset(mediaAssetId);
    }

    private static MediaReferenceVO toView(com.starrainnotes.media.vo.MediaReferenceVO vo) {
        return new MediaReferenceVO(vo.getMediaAssetId(), vo.getSourceModule(), vo.getSourceType(),
                vo.getSourceId(), vo.getUsageCode());
    }
}
