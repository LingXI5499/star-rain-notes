package com.starrainnotes.media.service.impl;

import com.starrainnotes.media.exception.MediaAssetNotActiveException;
import com.starrainnotes.media.exception.MediaAssetNotFoundException;
import com.starrainnotes.media.exception.MediaQueryInvalidException;
import com.starrainnotes.media.exception.MediaReferenceExistsException;
import com.starrainnotes.media.exception.MediaReferenceNotFoundException;
import com.starrainnotes.media.service.MediaReferenceService;
import com.starrainnotes.media.api.dto.MediaReferenceCommand;
import com.starrainnotes.media.enumeration.MediaStatus;
import com.starrainnotes.media.entity.MediaAssetEntity;
import com.starrainnotes.media.mapper.MediaAssetMapper;
import com.starrainnotes.media.utils.MediaReferenceCommands;
import com.starrainnotes.media.entity.MediaReferenceEntity;
import com.starrainnotes.media.mapper.MediaReferenceMapper;
import com.starrainnotes.media.vo.MediaReferenceVO;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * MED-003 / MED-004 / MED-007 的实现。
 *
 * 并发策略：attach 与 archive 都先对 MediaAsset 行加锁（SELECT ... FOR UPDATE），
 * 且锁顺序一致，因此不会出现“归档检查通过后又被 attach”，也不会两方互锁。
 *
 * 注意：对业务模块暴露的 MediaReferenceApi 由 service/impl 里的 MediaReferenceApiAdapter 转接。
 * 原因是模块间视图 MediaReferenceView 与模块内视图 MediaReferenceVO 返回类型不同，
 * Java 不允许同一个类用同一签名实现两个返回类型不同的方法。
 */
@Service
public class MediaReferenceServiceImpl implements MediaReferenceService {

    private final MediaAssetMapper assetMapper;
    private final MediaReferenceMapper referenceMapper;

    public MediaReferenceServiceImpl(MediaAssetMapper assetMapper,
                                     MediaReferenceMapper referenceMapper) {
        this.assetMapper = assetMapper;
        this.referenceMapper = referenceMapper;
    }

    @Override
    @Transactional
    public void attach(MediaReferenceCommand command) {
        MediaReferenceCommand normalized = MediaReferenceCommands.normalize(command);

        // 先锁资产行：与 archive 串行化，否则可能刚检查完就被归档
        MediaAssetEntity asset = assetMapper.assetByIdForUpdate(normalized.getMediaAssetId());
        if (asset == null) {
            throw new MediaAssetNotFoundException();
        }
        if (!MediaStatus.ACTIVE_CODE.equals(asset.getStatus())) {
            throw new MediaAssetNotActiveException();
        }

        MediaReferenceEntity entity = new MediaReferenceEntity();
        entity.setMediaAssetId(normalized.getMediaAssetId());
        entity.setSourceModule(normalized.getSourceModule());
        entity.setSourceType(normalized.getSourceType());
        entity.setSourceId(normalized.getSourceId());
        entity.setUsageCode(normalized.getUsageCode());
        try {
            referenceMapper.insertReference(entity);
        } catch (DuplicateKeyException ex) {
            // 命中唯一键：同一 (资产, 来源, 用途) 已经登记过
            throw new MediaReferenceExistsException();
        }
    }

    @Override
    @Transactional
    public void detach(MediaReferenceCommand command) {
        MediaReferenceCommand normalized = MediaReferenceCommands.normalize(command);

        // 只解除引用：绝不删除 MediaAsset，也绝不删除文件
        int deleted = referenceMapper.deleteReference(normalized.getMediaAssetId(), normalized.getSourceModule(),
                normalized.getSourceType(), normalized.getSourceId(), normalized.getUsageCode());
        if (deleted == 0) {
            throw new MediaReferenceNotFoundException();
        }
    }

    @Override
    @Transactional
    public void detachAll(String sourceModule, String sourceType, Long sourceId) {
        String[] source = MediaReferenceCommands.normalizeSource(sourceModule, sourceType, sourceId);

        // 业务对象删除/归档时批量清理：与调用方在同一事务中执行
        referenceMapper.deleteAllBySource(source[0], source[1], sourceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MediaReferenceVO> listByAsset(Long mediaAssetId) {
        if (mediaAssetId == null || mediaAssetId <= 0) {
            throw new MediaQueryInvalidException("mediaAssetId 必须为正整数");
        }
        return referenceMapper.selectByMediaAssetId(mediaAssetId).stream()
                .map(MediaReferenceVO::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByAsset(Long mediaAssetId) {
        // 归档前判断与详情展示共用；非法 ID 直接视为 0 条引用
        if (mediaAssetId == null || mediaAssetId <= 0) {
            return 0L;
        }
        return referenceMapper.countByMediaAssetId(mediaAssetId);
    }
}
