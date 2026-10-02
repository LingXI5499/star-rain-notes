package com.starrainnotes.media.service.impl;

import com.starrainnotes.media.exception.MediaAssetInUseException;
import com.starrainnotes.media.exception.MediaAssetNotActiveException;
import com.starrainnotes.media.exception.MediaAssetNotFoundException;
import com.starrainnotes.media.exception.MediaQueryInvalidException;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.media.api.MediaAssetApi;
import com.starrainnotes.media.api.dto.MediaAssetSummary;
import com.starrainnotes.media.dto.MediaQueryDTO;
import com.starrainnotes.media.entity.MediaAssetEntity;
import com.starrainnotes.media.mapper.MediaAssetMapper;
import com.starrainnotes.media.mapper.MediaReferenceMapper;
import com.starrainnotes.media.service.MediaAssetService;
import com.starrainnotes.media.enumeration.MediaAccessLevel;
import com.starrainnotes.media.enumeration.MediaStatus;
import com.starrainnotes.media.enumeration.MediaType;
import com.starrainnotes.media.vo.MediaAssetDetailVO;
import com.starrainnotes.media.vo.MediaAssetVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * MED-002 / MED-006 与归档生命周期实现，同时作为 MediaAssetApi 供业务模块调用。
 *
 * 归档的并发策略：
 *   1. 先对 MediaAsset 行加锁（assetByIdForUpdate），与 attach 的锁顺序一致；
 *   2. 锁内统计引用数；
 *   3. 有引用则拒绝，无引用才置为 ARCHIVED。
 * 这样就不会出现“检查通过后立刻被重新引用”的空档。
 */
@Service
public class MediaAssetServiceImpl implements MediaAssetService, MediaAssetApi {

    private static final int MAX_PAGE_SIZE = 100;

    // 可筛选取值直接由枚举派生，新增媒体大类时无需改动这里
    private static final Set<String> MEDIA_TYPES = Stream.of(MediaType.values())
            .map(Enum::name).collect(Collectors.toUnmodifiableSet());
    private static final Set<String> STATUSES = Stream.of(MediaStatus.values())
            .map(Enum::name).collect(Collectors.toUnmodifiableSet());
    private static final Set<String> ACCESS_LEVELS = Stream.of(MediaAccessLevel.values())
            .map(Enum::name).collect(Collectors.toUnmodifiableSet());

    private final MediaAssetMapper assetMapper;
    private final MediaReferenceMapper referenceMapper;

    public MediaAssetServiceImpl(MediaAssetMapper assetMapper,
                                 MediaReferenceMapper referenceMapper) {
        this.assetMapper = assetMapper;
        this.referenceMapper = referenceMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<MediaAssetVO> page(MediaQueryDTO query) {
        MediaQueryDTO condition = query == null ? new MediaQueryDTO() : query;
        int page = Math.max(1, condition.getPage());
        int pageSize = condition.getPageSize();
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new MediaQueryInvalidException("pageSize 必须在 1 到 " + MAX_PAGE_SIZE + " 之间");
        }
        String keyword = trimToNull(condition.getKeyword());
        String mediaType = normalizeFilter(condition.getMediaType(), MEDIA_TYPES, "mediaType");
        String status = normalizeFilter(condition.getStatus(), STATUSES, "status");
        String accessLevel = normalizeFilter(condition.getAccessLevel(), ACCESS_LEVELS, "accessLevel");

        long total = assetMapper.assetPageCount(keyword, mediaType, status, accessLevel);
        List<MediaAssetVO> items = assetMapper
                .assetPage(keyword, mediaType, status, accessLevel, (page - 1) * pageSize, pageSize)
                .stream()
                // 列表不逐个统计同内容素材数：那是上传时的重复提示，不是列表字段
                .map(entity -> MediaAssetVO.from(entity, 0L))
                .toList();
        return new PageResult<>(items, total, page, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public MediaAssetDetailVO getDetail(Long mediaAssetId) {
        MediaAssetEntity asset = requireAsset(mediaAssetId);
        long referenceCount = referenceMapper.countByMediaAssetId(mediaAssetId);
        return MediaAssetDetailVO.from(asset, referenceCount);
    }

    @Override
    @Transactional
    public void archive(Long mediaAssetId) {
        MediaAssetEntity asset = assetMapper.assetByIdForUpdate(mediaAssetId);
        if (asset == null) {
            throw new MediaAssetNotFoundException();
        }
        if (!MediaStatus.ACTIVE_CODE.equals(asset.getStatus())) {
            throw new MediaAssetNotActiveException();
        }

        // 引用完整性优先于管理便利：即使超级管理员也不能绕过
        long referenceCount = referenceMapper.countByMediaAssetId(mediaAssetId);
        if (referenceCount > 0) {
            throw new MediaAssetInUseException(referenceCount);
        }

        int updated = assetMapper.archiveAsset(mediaAssetId, LocalDateTime.now());
        if (updated == 0) {
            // 锁内状态被改动属于异常情况，宁可报错也不静默成功
            throw new MediaAssetNotActiveException();
        }
    }

    @Override
    @Transactional
    public void restore(Long mediaAssetId) {
        MediaAssetEntity asset = assetMapper.assetByIdForUpdate(mediaAssetId);
        if (asset == null) {
            throw new MediaAssetNotFoundException();
        }
        if (!MediaStatus.ARCHIVED_CODE.equals(asset.getStatus())) {
            throw new MediaAssetNotActiveException();
        }
        int updated = assetMapper.restoreAsset(mediaAssetId);
        if (updated == 0) {
            throw new MediaAssetNotActiveException();
        }
    }

    @Override
    @Transactional
    public void changeAccessLevel(Long mediaAssetId, MediaAccessLevel accessLevel) {
        requireAsset(mediaAssetId);
        assetMapper.updateAccessLevel(mediaAssetId, accessLevel.name());
    }

    // ---------- MediaAssetApi ----------

    @Override
    @Transactional(readOnly = true)
    public boolean exists(Long mediaAssetId) {
        return mediaAssetId != null && assetMapper.assetById(mediaAssetId) != null;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isActive(Long mediaAssetId) {
        MediaAssetEntity asset = mediaAssetId == null ? null : assetMapper.assetById(mediaAssetId);
        return asset != null && MediaStatus.ACTIVE_CODE.equals(asset.getStatus());
    }

    // 查询语义：不存在返回 null，由调用方决定是忽略还是报错
    @Override
    @Transactional(readOnly = true)
    public MediaAssetSummary get(Long mediaAssetId) {
        MediaAssetEntity asset = mediaAssetId == null ? null : assetMapper.assetById(mediaAssetId);
        return asset == null ? null : summary(asset);
    }

    @Override
    @Transactional(readOnly = true)
    public void assertUsable(Long mediaAssetId) {
        MediaAssetEntity asset = mediaAssetId == null ? null : assetMapper.assetById(mediaAssetId);
        if (asset == null) {
            throw new MediaAssetNotFoundException();
        }
        if (!MediaStatus.ACTIVE_CODE.equals(asset.getStatus())) {
            throw new MediaAssetNotActiveException();
        }
    }

    private MediaAssetEntity requireAsset(Long mediaAssetId) {
        MediaAssetEntity asset = mediaAssetId == null ? null : assetMapper.assetById(mediaAssetId);
        if (asset == null) {
            throw new MediaAssetNotFoundException();
        }
        return asset;
    }

    private static MediaAssetSummary summary(MediaAssetEntity asset) {
        return new MediaAssetSummary(asset.getId(), asset.getOriginalName(), asset.getMediaType(),
                asset.getMimeType(), asset.getWidth(), asset.getHeight(), asset.getAccessLevel(),
                asset.getStatus(), MediaAssetVO.contentUrl(asset.getId()));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // 空白视为“不筛选”；非空但取值非法时明确报错，不静默忽略
    private static String normalizeFilter(String value, Set<String> allowed, String label) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        String normalized = trimmed.toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw new MediaQueryInvalidException(label + " 取值非法：" + value);
        }
        return normalized;
    }
}
