package com.starrainnotes.media.api;

import com.starrainnotes.media.api.dto.MediaReferenceCommand;
import com.starrainnotes.media.api.dto.MediaReferenceView;
import java.util.List;

/*
 * 业务模块建立 / 解除 / 查询媒体引用的唯一入口。
 *
 * Tutorial / Blog / Portfolio / Profile 禁止直接访问 Media 的 Mapper、Entity 或数据表，
 * 也禁止自己往 sr_media_reference 写数据。
 */
public interface MediaReferenceApi {

    /*
     * 建立引用。要求媒体存在且为 ACTIVE，相同引用不能重复登记。
     * 应在调用方业务模块的同一数据库事务中执行，保证业务对象与引用一起提交或一起回滚。
     */
    void attach(MediaReferenceCommand command);

    // 只解除引用：绝不删除 MediaAsset，也绝不删除文件
    void detach(MediaReferenceCommand command);

    // 业务对象删除 / 归档时批量解除该来源的全部引用
    void detachAll(String sourceModule, String sourceType, Long sourceId);

    List<MediaReferenceView> listByAsset(Long mediaAssetId);

    long countByAsset(Long mediaAssetId);
}
