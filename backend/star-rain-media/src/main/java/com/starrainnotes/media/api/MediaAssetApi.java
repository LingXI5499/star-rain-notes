/*
 * MediaAssetApi：媒体模块的跨模块接口文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 * 主要协作类型：MediaAssetSummary。
 * 阅读入口：exists（判断是否存在媒体资产）；isActive（判断是否有效）；get（读取媒体资产）；assertUsable（校验Usable）。
 */

package com.starrainnotes.media.api;

import com.starrainnotes.media.api.dto.MediaAssetSummary;

/*
 * 业务模块校验媒体可用性的唯一入口。
 *
 * 业务模块保存 mediaAssetId 后，需要能确认这个媒体还在、还是 ACTIVE；
 * 但不需要也不允许直接读 Media 的表。
 */
/**
 * 媒体资产接口，属于媒体模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
public interface MediaAssetApi {

    /**
     * 判断是否存在媒体资产。
     *
     * @param mediaAssetId 关联的媒体资产标识；类型为 <code>Long</code>
     * @return 本方法条件是否成立的判断结果
     */
    boolean exists(Long mediaAssetId);

    /**
     * 判断是否有效。
     *
     * @param mediaAssetId 关联的媒体资产标识；类型为 <code>Long</code>
     * @return 本方法条件是否成立的判断结果
     */
    boolean isActive(Long mediaAssetId);

    // 查询型接口：媒体不存在时返回 null，不抛异常
    /**
     * 读取媒体资产。
     *
     * @param mediaAssetId 关联的媒体资产标识；类型为 <code>Long</code>
     * @return 类型为 <code>MediaAssetSummary</code> 的处理结果
     */
    MediaAssetSummary get(Long mediaAssetId);

    // 断言型接口：媒体不存在或不是 ACTIVE 时抛出对应业务错误
    /**
     * 校验Usable。
     *
     * @param mediaAssetId 关联的媒体资产标识；类型为 <code>Long</code>
     */
    void assertUsable(Long mediaAssetId);
}
