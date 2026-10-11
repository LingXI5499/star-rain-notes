/*
 * MediaAssetServiceImpl：媒体模块的业务实现文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：MediaAssetInUseException、MediaAssetNotActiveException、MediaAssetNotFoundException、MediaQueryInvalidException、PageResult、MediaAssetApi、MediaAssetSummary。
 * 阅读入口：page（分页读取媒体资产）；getDetail（读取详情）；archive（archive处理入口）；restore（恢复媒体资产）。
 */

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
/**
 * 媒体资产业务服务实现，属于媒体模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
public class MediaAssetServiceImpl implements MediaAssetService, MediaAssetApi {

    /**
     * 大小。
     * 初始值为 <code>100</code>。
     */
    private static final int MAX_PAGE_SIZE = 100;

    // 可筛选取值直接由枚举派生，新增媒体大类时无需改动这里
    /**
     * TYPES。
     */
    private static final Set<String> MEDIA_TYPES = Stream.of(MediaType.values())
            .map(Enum::name).collect(Collectors.toUnmodifiableSet());
    /**
     * STATUSES。
     */
    private static final Set<String> STATUSES = Stream.of(MediaStatus.values())
            .map(Enum::name).collect(Collectors.toUnmodifiableSet());
    /**
     * LEVELS。
     */
    private static final Set<String> ACCESS_LEVELS = Stream.of(MediaAccessLevel.values())
            .map(Enum::name).collect(Collectors.toUnmodifiableSet());

    /**
     * 资产数据映射。
     * 协作类型：<code>MediaAssetMapper</code>。
     */
    private final MediaAssetMapper assetMapper;
    /**
     * 引用数据映射。
     * 协作类型：<code>MediaReferenceMapper</code>。
     */
    private final MediaReferenceMapper referenceMapper;

    /**
     * 构造MediaAssetServiceImpl，接收本类使用的依赖或初始化参数。
     *
     * @param assetMapper 资产数据映射；类型为 <code>MediaAssetMapper</code>
     * @param referenceMapper 引用数据映射；类型为 <code>MediaReferenceMapper</code>
     */
    public MediaAssetServiceImpl(MediaAssetMapper assetMapper,
                                 MediaReferenceMapper referenceMapper) {
        this.assetMapper = assetMapper;
        this.referenceMapper = referenceMapper;
    }

    /**
     * 分页读取媒体资产。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>assetMapper.assetPageCount</code>、<code>assetMapper.assetPage</code>。
     * 实现中的显式失败类型包括 <code>MediaQueryInvalidException</code>；具体触发条件见对应分支。
     *
     * @param query 查询参数；类型为 <code>MediaQueryDTO</code>
     * @return 类型为 <code>PageResult&lt;MediaAssetVO&gt;</code> 的处理结果
     */
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

    /**
     * 读取详情。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>referenceMapper.countByMediaAssetId</code>。
     *
     * @param mediaAssetId 关联的媒体资产标识；类型为 <code>Long</code>
     * @return 类型为 <code>MediaAssetDetailVO</code> 的处理结果
     */
    @Override
    @Transactional(readOnly = true)
    public MediaAssetDetailVO getDetail(Long mediaAssetId) {
        MediaAssetEntity asset = requireAsset(mediaAssetId);
        long referenceCount = referenceMapper.countByMediaAssetId(mediaAssetId);
        return MediaAssetDetailVO.from(asset, referenceCount);
    }

    /**
     * archive处理入口。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>assetMapper.assetByIdForUpdate</code>、<code>referenceMapper.countByMediaAssetId</code>、<code>assetMapper.archiveAsset</code>。
     * 实现中的显式失败类型包括 <code>MediaAssetNotFoundException</code>、<code>MediaAssetNotActiveException</code>、<code>MediaAssetInUseException</code>；具体触发条件见对应分支。
     *
     * @param mediaAssetId 关联的媒体资产标识；类型为 <code>Long</code>
     */
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

    /**
     * 恢复媒体资产。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>assetMapper.assetByIdForUpdate</code>、<code>assetMapper.restoreAsset</code>。
     * 实现中的显式失败类型包括 <code>MediaAssetNotFoundException</code>、<code>MediaAssetNotActiveException</code>；具体触发条件见对应分支。
     *
     * @param mediaAssetId 关联的媒体资产标识；类型为 <code>Long</code>
     */
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

    /**
     * change访问Level处理入口。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>assetMapper.updateAccessLevel</code>。
     *
     * @param mediaAssetId 关联的媒体资产标识；类型为 <code>Long</code>
     * @param accessLevel 媒体访问级别；类型为 <code>MediaAccessLevel</code>
     */
    @Override
    @Transactional
    public void changeAccessLevel(Long mediaAssetId, MediaAccessLevel accessLevel) {
        requireAsset(mediaAssetId);
        assetMapper.updateAccessLevel(mediaAssetId, accessLevel.name());
    }

    // ---------- MediaAssetApi ----------

    /**
     * 判断是否存在媒体资产。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>assetMapper.assetById</code>。
     *
     * @param mediaAssetId 关联的媒体资产标识；类型为 <code>Long</code>
     * @return 本方法条件是否成立的判断结果
     */
    @Override
    @Transactional(readOnly = true)
    public boolean exists(Long mediaAssetId) {
        return mediaAssetId != null && assetMapper.assetById(mediaAssetId) != null;
    }

    /**
     * 判断是否有效。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>assetMapper.assetById</code>。
     *
     * @param mediaAssetId 关联的媒体资产标识；类型为 <code>Long</code>
     * @return 本方法条件是否成立的判断结果
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isActive(Long mediaAssetId) {
        MediaAssetEntity asset = mediaAssetId == null ? null : assetMapper.assetById(mediaAssetId);
        return asset != null && MediaStatus.ACTIVE_CODE.equals(asset.getStatus());
    }

    // 查询语义：不存在返回 null，由调用方决定是忽略还是报错
    /**
     * 读取媒体资产。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>assetMapper.assetById</code>。
     *
     * @param mediaAssetId 关联的媒体资产标识；类型为 <code>Long</code>
     * @return 类型为 <code>MediaAssetSummary</code> 的处理结果
     */
    @Override
    @Transactional(readOnly = true)
    public MediaAssetSummary get(Long mediaAssetId) {
        MediaAssetEntity asset = mediaAssetId == null ? null : assetMapper.assetById(mediaAssetId);
        return asset == null ? null : summary(asset);
    }

    /**
     * 校验Usable。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>assetMapper.assetById</code>。
     * 实现中的显式失败类型包括 <code>MediaAssetNotFoundException</code>、<code>MediaAssetNotActiveException</code>；具体触发条件见对应分支。
     *
     * @param mediaAssetId 关联的媒体资产标识；类型为 <code>Long</code>
     */
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

    /**
     * 检查所需资产。
     * 主要协作调用：<code>assetMapper.assetById</code>。
     * 实现中的显式失败类型包括 <code>MediaAssetNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param mediaAssetId 关联的媒体资产标识；类型为 <code>Long</code>
     * @return 类型为 <code>MediaAssetEntity</code> 的处理结果
     */
    private MediaAssetEntity requireAsset(Long mediaAssetId) {
        MediaAssetEntity asset = mediaAssetId == null ? null : assetMapper.assetById(mediaAssetId);
        if (asset == null) {
            throw new MediaAssetNotFoundException();
        }
        return asset;
    }

    /**
     * 摘要处理入口。
     * 关键处理步骤调用 <code>asset.getId</code>、<code>asset.getOriginalName</code>、<code>asset.getMediaType</code>、<code>asset.getMimeType</code>。
     *
     * @param asset 资产；类型为 <code>MediaAssetEntity</code>
     * @return 类型为 <code>MediaAssetSummary</code> 的处理结果
     */
    private static MediaAssetSummary summary(MediaAssetEntity asset) {
        return new MediaAssetSummary(asset.getId(), asset.getOriginalName(), asset.getMediaType(),
                asset.getMimeType(), asset.getWidth(), asset.getHeight(), asset.getAccessLevel(),
                asset.getStatus(), MediaAssetVO.contentUrl(asset.getId()));
    }

    /**
     * 去除首尾空白目标空值处理入口。
     * 关键处理步骤调用 <code>value.trim</code>、<code>trimmed.isEmpty</code>。
     *
     * @param value 值；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // 空白视为“不筛选”；非空但取值非法时明确报错，不静默忽略
    /**
     * 规范化筛选。
     * 关键处理步骤调用 <code>trimToNull</code>、<code>trimmed.toUpperCase</code>、<code>allowed.contains</code>。
     * 实现中的显式失败类型包括 <code>MediaQueryInvalidException</code>；具体触发条件见对应分支。
     *
     * @param value 值；类型为 <code>String</code>
     * @param allowed allowed；类型为 <code>Set&lt;String&gt;</code>
     * @param label 标签；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
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
