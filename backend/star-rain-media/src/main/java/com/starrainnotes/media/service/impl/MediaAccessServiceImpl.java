/*
 * MediaAccessServiceImpl：媒体模块的业务实现文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：MediaAccessDeniedException、MediaAssetArchivedException、MediaAssetNotFoundException、MediaContentMissingException、CurrentActorApi、MediaAssetEntity、MediaAssetMapper。
 * 阅读入口：open（打开媒体访问）。
 */

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
/**
 * 媒体访问业务服务实现，属于媒体模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
public class MediaAccessServiceImpl implements MediaAccessService {

    /**
     * 资产数据映射。
     * 协作类型：<code>MediaAssetMapper</code>。
     */
    private final MediaAssetMapper assetMapper;
    /**
     * 存储。
     */
    private final MediaStorage storage;
    /**
     * 提供当前认证操作者身份的跨模块接口。
     * 协作类型：<code>CurrentActorApi</code>。
     */
    private final CurrentActorApi currentActorApi;

    /**
     * 构造MediaAccessServiceImpl，接收本类使用的依赖或初始化参数。
     *
     * @param assetMapper 资产数据映射；类型为 <code>MediaAssetMapper</code>
     * @param storage 存储；类型为 <code>MediaStorage</code>
     * @param currentActorApi 提供当前认证操作者身份的跨模块接口；类型为 <code>CurrentActorApi</code>
     */
    public MediaAccessServiceImpl(MediaAssetMapper assetMapper,
                                  MediaStorage storage,
                                  CurrentActorApi currentActorApi) {
        this.assetMapper = assetMapper;
        this.storage = storage;
        this.currentActorApi = currentActorApi;
    }

    /**
     * 打开媒体访问。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>assetMapper.assetById</code>、<code>storage.size</code>、<code>storage.open</code>、<code>storage.openRange</code>。
     * 实现中的显式失败类型包括 <code>MediaAssetNotFoundException</code>、<code>MediaContentMissingException</code>；具体触发条件见对应分支。
     *
     * @param mediaAssetId 关联的媒体资产标识；类型为 <code>Long</code>
     * @param rangeHeader 范围响应头；类型为 <code>String</code>
     * @return 类型为 <code>MediaContentVO</code> 的处理结果
     */
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

    /**
     * 校验授权媒体访问。
     * 关键处理步骤调用 <code>MediaStatus.ARCHIVED_CODE.equals</code>、<code>asset.getStatus</code>、<code>canReadRestricted</code>、<code>MediaAccessLevel.PROTECTED.name</code>。
     * 实现中的显式失败类型包括 <code>MediaAssetArchivedException</code>、<code>MediaAccessDeniedException</code>；具体触发条件见对应分支。
     *
     * @param asset 资产；类型为 <code>MediaAssetEntity</code>
     */
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

    /**
     * 判断能否读取Restricted。
     * 主要协作调用：<code>currentActorApi.currentOptional</code>。
     * @return 本方法条件是否成立的判断结果
     */
    private boolean canReadRestricted() {
        return currentActorApi.currentOptional()
                .map(actor -> actor.getPermissions().contains(MediaPermissions.READ))
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public MediaContentVO describe(Long mediaAssetId) {
        if (mediaAssetId == null || mediaAssetId <= 0) throw new MediaAssetNotFoundException();
        var asset = assetMapper.assetById(mediaAssetId);
        if (asset == null) throw new MediaAssetNotFoundException();
        authorize(asset);
        long size = storage.size(asset.getStorageKey());
        if (size < 0) throw new MediaContentMissingException();
        return new MediaContentVO(asset.getId(), MediaType.of(asset.getMediaType()), asset.getMimeType(),
                asset.getOriginalName(), size, asset.getSha256(), asset.getAccessLevel(), null, null);
    }
}
