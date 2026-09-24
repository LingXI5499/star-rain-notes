package com.starrainnotes.media.api;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.media.entity.MediaAsset;
import com.starrainnotes.media.mapper.MediaAssetMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/** Small content-facing media capability. */
@Component
public class MediaAssetPort {
    private final MediaAssetMapper mapper;
    public MediaAssetPort(MediaAssetMapper mapper) { this.mapper = mapper; }
    public boolean isImage(Long id) {
        Long count = mapper.selectCount(new LambdaQueryWrapper<MediaAsset>()
                .eq(MediaAsset::getId, id).eq(MediaAsset::getAssetType, "IMAGE"));
        return count != null && count > 0;
    }
    public boolean isType(Long id, String type) {
        if (id == null || type == null) return false;
        Long count = mapper.selectCount(new LambdaQueryWrapper<MediaAsset>()
                .eq(MediaAsset::getId, id).eq(MediaAsset::getAssetType, type));
        return count != null && count > 0;
    }
    public String assetType(Long id) {
        if (id == null) return null;
        MediaAsset asset = mapper.selectById(id);
        return asset == null ? null : asset.getAssetType();
    }
    public String publicUrl(Long id) {
        if (id == null) return null;
        MediaAsset asset = mapper.selectById(id);
        return asset == null ? null : asset.getPublicUrl();
    }
    public Map<Long, String> publicUrls(Collection<Long> ids) {
        var distinct = ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) return Map.of();
        return mapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(MediaAsset::getId, MediaAsset::getPublicUrl));
    }
    public void requireImageIfPresent(Long id) {
        if (id != null && !isImage(id)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_COVER_MEDIA",
                    "Invalid cover", "The selected cover must be an existing image asset.");
        }
    }
}
