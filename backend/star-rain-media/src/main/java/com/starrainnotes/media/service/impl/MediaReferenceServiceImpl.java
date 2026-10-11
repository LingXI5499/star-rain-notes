/*
 * MediaReferenceServiceImpl：媒体模块的业务实现文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：MediaAssetNotActiveException、MediaAssetNotFoundException、MediaQueryInvalidException、MediaReferenceExistsException、MediaReferenceNotFoundException、MediaReferenceService、MediaReferenceCommand。
 * 阅读入口：attach（建立关联媒体引用）；detach（解除关联媒体引用）；detachAll（解除关联全部）；listByAsset（列出依据资产）。
 */

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
 * 注意：对业务模块暴露的 MediaReferenceApi 由 api/impl 里的 MediaReferenceApiAdapter 转接。
 * 原因是模块间视图 MediaReferenceVO（media.api.vo） 与模块内视图 MediaReferenceVO 返回类型不同，
 * Java 不允许同一个类用同一签名实现两个返回类型不同的方法。
 */
/**
 * 媒体引用业务服务实现，属于媒体模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
public class MediaReferenceServiceImpl implements MediaReferenceService {

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
     * 构造MediaReferenceServiceImpl，接收本类使用的依赖或初始化参数。
     *
     * @param assetMapper 资产数据映射；类型为 <code>MediaAssetMapper</code>
     * @param referenceMapper 引用数据映射；类型为 <code>MediaReferenceMapper</code>
     */
    public MediaReferenceServiceImpl(MediaAssetMapper assetMapper,
                                     MediaReferenceMapper referenceMapper) {
        this.assetMapper = assetMapper;
        this.referenceMapper = referenceMapper;
    }

    /**
     * 建立关联媒体引用。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>assetMapper.assetByIdForUpdate</code>、<code>referenceMapper.insertReference</code>。
     * 实现中的显式失败类型包括 <code>MediaAssetNotFoundException</code>、<code>MediaAssetNotActiveException</code>、<code>MediaReferenceExistsException</code>；具体触发条件见对应分支。
     *
     * @param command command；类型为 <code>MediaReferenceCommand</code>
     */
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

    /**
     * 解除关联媒体引用。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>referenceMapper.deleteReference</code>。
     * 实现中的显式失败类型包括 <code>MediaReferenceNotFoundException</code>；具体触发条件见对应分支。
     *
     * @param command command；类型为 <code>MediaReferenceCommand</code>
     */
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

    /**
     * 解除关联全部。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>referenceMapper.deleteAllBySource</code>。
     *
     * @param sourceModule 引用或数据来源所属模块；类型为 <code>String</code>
     * @param sourceType 来源对象的业务类型；类型为 <code>String</code>
     * @param sourceId 来源对象的业务标识；类型为 <code>Long</code>
     */
    @Override
    @Transactional
    public void detachAll(String sourceModule, String sourceType, Long sourceId) {
        String[] source = MediaReferenceCommands.normalizeSource(sourceModule, sourceType, sourceId);

        // 业务对象删除/归档时批量清理：与调用方在同一事务中执行
        referenceMapper.deleteAllBySource(source[0], source[1], sourceId);
    }

    /**
     * 列出依据资产。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>referenceMapper.selectByMediaAssetId</code>。
     * 实现中的显式失败类型包括 <code>MediaQueryInvalidException</code>；具体触发条件见对应分支。
     *
     * @param mediaAssetId 关联的媒体资产标识；类型为 <code>Long</code>
     * @return 本次处理得到的条目集合
     */
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

    /**
     * 统计依据资产。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>referenceMapper.countByMediaAssetId</code>。
     *
     * @param mediaAssetId 关联的媒体资产标识；类型为 <code>Long</code>
     * @return 类型为 <code>long</code> 的处理结果
     */
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
