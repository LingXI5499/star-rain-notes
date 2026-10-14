/*
 * MediaReferenceService：媒体模块的业务契约文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：MediaReferenceCommand、MediaReferenceVO。
 * 阅读入口：attach（建立关联媒体引用）；detach（解除关联媒体引用）；detachAll（解除关联全部）；listByAsset（列出依据资产）。
 */

package com.starrainnotes.media.service;

import com.starrainnotes.media.api.dto.MediaReferenceCommand;
import com.starrainnotes.media.vo.MediaReferenceVO;
import java.util.List;

// 媒体引用的模块内服务契约。对业务模块暴露的入口是 api 包里的 MediaReferenceApi
/**
 * 媒体引用业务服务，属于媒体模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface MediaReferenceService {
    void assertPublicContent(String markdown);

    /**
     * 建立关联媒体引用。
     *
     * @param command command；类型为 <code>MediaReferenceCommand</code>
     */
    void attach(MediaReferenceCommand command);

    /**
     * 解除关联媒体引用。
     *
     * @param command command；类型为 <code>MediaReferenceCommand</code>
     */
    void detach(MediaReferenceCommand command);

    /**
     * 解除关联全部。
     *
     * @param sourceModule 引用或数据来源所属模块；类型为 <code>String</code>
     * @param sourceType 来源对象的业务类型；类型为 <code>String</code>
     * @param sourceId 来源对象的业务标识；类型为 <code>Long</code>
     */
    void detachAll(String sourceModule, String sourceType, Long sourceId);

    /**
     * 列出依据资产。
     *
     * @param mediaAssetId 关联的媒体资产标识；类型为 <code>Long</code>
     * @return 本次处理得到的条目集合
     */
    List<MediaReferenceVO> listByAsset(Long mediaAssetId);

    /**
     * 统计依据资产。
     *
     * @param mediaAssetId 关联的媒体资产标识；类型为 <code>Long</code>
     * @return 类型为 <code>long</code> 的处理结果
     */
    long countByAsset(Long mediaAssetId);
}
