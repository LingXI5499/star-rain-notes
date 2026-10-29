package com.starrainnotes.media.service.impl;

import com.starrainnotes.media.service.EditorImageService;
import com.starrainnotes.media.service.MediaUploadService;
import com.starrainnotes.media.remote.RemoteImageFetcher;

import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.account.api.PermissionQueryApi;
import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.media.enumeration.MediaAccessLevel;
import com.starrainnotes.media.exception.MediaReferenceInvalidException;
import com.starrainnotes.media.mapper.MediaAssetMapper;
import com.starrainnotes.media.vo.MediaAssetVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Images explicitly inserted in content become reusable public media. */
@Service
@RequiredArgsConstructor
public class EditorImageServiceImpl implements EditorImageService {
    private final MediaAssetMapper assets;
    private final MediaUploadService uploads;
    private final CurrentActorApi actors;
    private final PermissionQueryApi permissions;
    private final RemoteImageFetcher remote;

    @Override
    @Transactional
    public MediaAssetVO prepare(Long id) {
        var asset = assets.assetByIdForUpdate(id);
        if (asset == null || !"ACTIVE".equals(asset.getStatus()) || !"IMAGE".equals(asset.getMediaType())) {
            throw new MediaReferenceInvalidException("请选择可用的图片。");
        }
        if (!"PUBLIC".equals(asset.getAccessLevel())) {
            Long account = actors.current().getAccountId();
            if (!account.equals(asset.getUploadedByAccountId())
                    && !permissions.hasPermission(account, "media:access-manage")) {
                throw new ApiException("FORBIDDEN", "无权公开其他账户的受保护图片。", 403);
            }
            assets.updateAccessLevel(id, "PUBLIC");
            asset.setAccessLevel("PUBLIC");
        }
        return MediaAssetVO.from(asset, assets.countBySha256(asset.getSha256()));
    }

    @Override
    public MediaAssetVO importUrl(String url) {
        return uploads.upload(remote.fetch(url), MediaAccessLevel.PUBLIC);
    }
}
