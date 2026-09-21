package com.starrainnotes.media.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.starrainnotes.media.dto.MediaAssetView;
import com.starrainnotes.media.dto.MediaPageView;
import com.starrainnotes.media.dto.MediaSummaryView;
import com.starrainnotes.media.entity.MediaAsset;
import com.starrainnotes.media.mapper.MediaAssetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** Read-side media-library listing, filtering and inventory summary. */
@Service
@RequiredArgsConstructor
public class MediaQueryService {

    private final MediaAssetMapper mapper;
    private final MediaService mediaService;

    public MediaPageView list(int page, int pageSize, String query, String assetType) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), 50);
        LambdaQueryWrapper<MediaAsset> wrapper = new LambdaQueryWrapper<MediaAsset>()
                .eq(assetType != null && !assetType.isBlank(), MediaAsset::getAssetType, assetType)
                .like(query != null && !query.isBlank(), MediaAsset::getOriginalName, query)
                .orderByDesc(MediaAsset::getId);
        Page<MediaAsset> result = mapper.selectPage(new Page<>(safePage, safeSize), wrapper);
        List<MediaAssetView> items = result.getRecords().stream().map(this::toView).toList();
        long total = result.getTotal();
        int totalPages = total == 0 ? 0 : (int) ((total + safeSize - 1) / safeSize);
        return new MediaPageView(items, safePage, safeSize, total, totalPages);
    }

    public MediaSummaryView summary() {
        return new MediaSummaryView(
                countByType(null), countByType("IMAGE"), countByType("AUDIO"),
                countByType("DOCUMENT"), countByType("ARCHIVE"));
    }

    private long countByType(String assetType) {
        Long count = mapper.selectCount(new LambdaQueryWrapper<MediaAsset>()
                .eq(assetType != null, MediaAsset::getAssetType, assetType));
        return count == null ? 0 : count;
    }

    private MediaAssetView toView(MediaAsset asset) {
        return new MediaAssetView(
                asset.getId(), asset.getAssetType(), asset.getOriginalName(), asset.getMimeType(),
                asset.getExtension(), asset.getSizeBytes(), asset.getWidth(), asset.getHeight(),
                asset.getPublicUrl(), mediaService.srcSetOf(asset), asset.getCreatedAt());
    }
}
