/*
 * MediaReferenceApi：媒体模块的跨模块接口文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 * 主要协作类型：MediaReferenceCommand、MediaReferenceVO。
 * 阅读入口：attach（建立关联媒体引用）；detach（解除关联媒体引用）；detachAll（解除关联全部）；listByAsset（列出依据资产）。
 */

package com.starrainnotes.media.api;

import com.starrainnotes.media.api.dto.MediaReferenceCommand;
import com.starrainnotes.media.api.vo.MediaReferenceVO;
import java.util.List;

/*
 * 业务模块建立 / 解除 / 查询媒体引用的唯一入口。
 *
 * Tutorial / Blog / Portfolio / Profile 禁止直接访问 Media 的 Mapper、Entity 或数据表，
 * 也禁止自己往 sr_media_reference 写数据。
 */
/**
 * 媒体引用接口，属于媒体模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
public interface MediaReferenceApi {

    /*
     * 建立引用。要求媒体存在且为 ACTIVE，相同引用不能重复登记。
     * 应在调用方业务模块的同一数据库事务中执行，保证业务对象与引用一起提交或一起回滚。
     */
    /**
     * 建立关联媒体引用。
     *
     * @param command command；类型为 <code>MediaReferenceCommand</code>
     */
    void attach(MediaReferenceCommand command);

    // 只解除引用：绝不删除 MediaAsset，也绝不删除文件
    /**
     * 解除关联媒体引用。
     *
     * @param command command；类型为 <code>MediaReferenceCommand</code>
     */
    void detach(MediaReferenceCommand command);

    // 业务对象删除 / 归档时批量解除该来源的全部引用
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
