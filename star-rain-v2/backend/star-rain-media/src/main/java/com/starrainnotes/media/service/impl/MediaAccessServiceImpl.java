package com.starrainnotes.media.service.impl;

import com.starrainnotes.media.exception.MediaAccessDeniedException;
import com.starrainnotes.media.exception.MediaAssetArchivedException;
import com.starrainnotes.media.exception.MediaAssetNotFoundException;
import com.starrainnotes.media.exception.MediaContentMissingException;
import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.media.entity.MediaAssetEntity;
import com.starrainnotes.media.mapper.MediaAssetMapper;
import com.starrainnotes.media.service.MediaAccessService;
import com.starrainnotes.media.storage.MediaStorage;
import com.starrainnotes.media.utils.HttpByteRange;
import com.starrainnotes.media.enumeration.MediaAccessLevel;
import com.starrainnotes.media.vo.MediaContentVO;
import com.starrainnotes.media.constant.MediaPermissions;
import com.starrainnotes.media.enumeration.MediaStatus;
import com.starrainnotes.media.enumeration.MediaType;
import java.io.InputStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * MED-005 实现。
 *
 * 读取授权规则：
 *   ACTIVE   + PUBLIC    → 任何人可读（含匿名 Visitor）
 *   ACTIVE   + PROTECTED → 必须已认证且具备 media:read
 *   ARCHIVED             → 只有具备 media:read 的后台用户可预览，前台拿不到
 *
 * 权限来自认证上下文，不来自请求参数；URL 层对 content 放行只是因为
 * accessLevel 是“每个资源自己的属性”，URL 层无法预判。
 *
 * Range 支持：音频/视频拖动进度条会发 Range 请求，这里按真实文件长度
 * 裁剪区间并交给 MediaStorage 定位读取，避免整份读进内存。
 */
@Service
public class MediaAccessServiceImpl implements MediaAccessService {

    private final MediaAssetMapper assetMapper;
    private final MediaStorage storage;
    private final CurrentActorApi currentActorApi;

    public MediaAccessServiceImpl(MediaAssetMapper assetMapper,
                                  MediaStorage storage,
                                  CurrentActorApi currentActorApi) {
        this.assetMapper = assetMapper;
        this.storage = storage;
        this.currentActorApi = currentActorApi;
    }

    @Override
    @Transactional(readOnly = true)
    public MediaContentVO open(Long mediaAssetId, String rangeHeader) {
        if (mediaAssetId == null || mediaAssetId <= 0) {
            throw new MediaAssetNotFoundException();
        }
        MediaAssetEntity asset = assetMapper.assetById(mediaAssetId);
        if (asset == null) {
            throw new MediaAssetNotFoundException();
        }
        authorize(asset);

        // 用真实文件长度而不是数据库字段：文件被外部改动时区间计算不能出错
        long totalSize = storage.size(asset.getStorageKey());
        if (totalSize < 0) {
            throw new MediaContentMissingException();
        }

        // 先解析区间再打开流：区间非法时直接 416，不会留下未关闭的流
        HttpByteRange range = HttpByteRange.parse(rangeHeader, totalSize);
        InputStream stream = range == null
                ? storage.open(asset.getStorageKey())
                : storage.openRange(asset.getStorageKey(), range.getStart(), range.length());

        return new MediaContentVO(
                asset.getId(),
                MediaType.of(asset.getMediaType()),
                asset.getMimeType(),
                asset.getOriginalName(),
                totalSize,
                asset.getSha256(),
                asset.getAccessLevel(),
                range,
                stream);
    }

    private void authorize(MediaAssetEntity asset) {
        if (MediaStatus.ARCHIVED_CODE.equals(asset.getStatus())) {
            // 归档资产默认不进入公开访问，后台可预览
            if (!canReadRestricted()) {
                throw new MediaAssetArchivedException();
            }
            return;
        }
        if (MediaAccessLevel.PROTECTED.name().equals(asset.getAccessLevel()) && !canReadRestricted()) {
            throw new MediaAccessDeniedException();
        }
    }

    private boolean canReadRestricted() {
        return currentActorApi.currentOptional()
                .map(actor -> actor.getPermissions().contains(MediaPermissions.READ))
                .orElse(false);
    }
}
