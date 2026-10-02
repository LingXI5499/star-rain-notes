package com.starrainnotes.media.api;

import com.starrainnotes.media.api.dto.MediaAssetSummary;

/*
 * 业务模块校验媒体可用性的唯一入口。
 *
 * 业务模块保存 mediaAssetId 后，需要能确认这个媒体还在、还是 ACTIVE；
 * 但不需要也不允许直接读 Media 的表。
 */
public interface MediaAssetApi {

    boolean exists(Long mediaAssetId);

    boolean isActive(Long mediaAssetId);

    // 查询型接口：媒体不存在时返回 null，不抛异常
    MediaAssetSummary get(Long mediaAssetId);

    // 断言型接口：媒体不存在或不是 ACTIVE 时抛出对应业务错误
    void assertUsable(Long mediaAssetId);
}
