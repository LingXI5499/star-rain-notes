/*
 * MediaReferenceApiAdapter：媒体模块的跨模块接口文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 * 主要协作类型：MediaReferenceApi、MediaReferenceCommand、MediaReferenceVO、MediaReferenceService。
 * 阅读入口：attach（建立关联媒体引用接口Adapter）；detach（解除关联媒体引用接口Adapter）；detachAll（解除关联全部）；listByAsset（列出依据资产）。
 */

package com.starrainnotes.media.api.impl;

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
/**
 * 媒体引用接口Adapter，属于媒体模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
@Component
public class MediaReferenceApiAdapter implements MediaReferenceApi {
    @Override public void assertPublicContent(String markdown) { referenceService.assertPublicContent(markdown); }

    /**
     * 引用业务服务。
     * 协作类型：<code>MediaReferenceService</code>。
     */
    private final MediaReferenceService referenceService;

    /**
     * 构造MediaReferenceApiAdapter，接收本类使用的依赖或初始化参数。
     *
     * @param referenceService 引用业务服务；类型为 <code>MediaReferenceService</code>
     */
    public MediaReferenceApiAdapter(MediaReferenceService referenceService) {
        this.referenceService = referenceService;
    }

    /**
     * 建立关联媒体引用接口Adapter。
     * 主要协作调用：<code>referenceService.attach</code>。
     *
     * @param command command；类型为 <code>MediaReferenceCommand</code>
     */
    @Override
    public void attach(MediaReferenceCommand command) {
        referenceService.attach(command);
    }

    /**
     * 解除关联媒体引用接口Adapter。
     * 主要协作调用：<code>referenceService.detach</code>。
     *
     * @param command command；类型为 <code>MediaReferenceCommand</code>
     */
    @Override
    public void detach(MediaReferenceCommand command) {
        referenceService.detach(command);
    }

    /**
     * 解除关联全部。
     * 主要协作调用：<code>referenceService.detachAll</code>。
     *
     * @param sourceModule 引用或数据来源所属模块；类型为 <code>String</code>
     * @param sourceType 来源对象的业务类型；类型为 <code>String</code>
     * @param sourceId 来源对象的业务标识；类型为 <code>Long</code>
     */
    @Override
    public void detachAll(String sourceModule, String sourceType, Long sourceId) {
        referenceService.detachAll(sourceModule, sourceType, sourceId);
    }

    /**
     * 列出依据资产。
     * 主要协作调用：<code>referenceService.listByAsset</code>。
     *
     * @param mediaAssetId 关联的媒体资产标识；类型为 <code>Long</code>
     * @return 本次处理得到的条目集合
     */
    @Override
    public List<MediaReferenceVO> listByAsset(Long mediaAssetId) {
        return referenceService.listByAsset(mediaAssetId).stream()
                .map(MediaReferenceApiAdapter::toView)
                .toList();
    }

    /**
     * 统计依据资产。
     * 主要协作调用：<code>referenceService.countByAsset</code>。
     *
     * @param mediaAssetId 关联的媒体资产标识；类型为 <code>Long</code>
     * @return 类型为 <code>long</code> 的处理结果
     */
    @Override
    public long countByAsset(Long mediaAssetId) {
        return referenceService.countByAsset(mediaAssetId);
    }

    /**
     * 转换为View。
     * 关键处理步骤调用 <code>vo.getMediaAssetId</code>、<code>vo.getSourceModule</code>、<code>vo.getSourceType</code>、<code>vo.getSourceId</code>。
     *
     * @param vo 接口展示数据对象；类型为 <code>com.starrainnotes.media.vo.MediaReferenceVO</code>
     * @return 类型为 <code>MediaReferenceVO</code> 的处理结果
     */
    private static MediaReferenceVO toView(com.starrainnotes.media.vo.MediaReferenceVO vo) {
        return new MediaReferenceVO(vo.getMediaAssetId(), vo.getSourceModule(), vo.getSourceType(),
                vo.getSourceId(), vo.getUsageCode());
    }
}
