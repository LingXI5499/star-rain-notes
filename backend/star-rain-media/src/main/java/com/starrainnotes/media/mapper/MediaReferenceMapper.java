/*
 * MediaReferenceMapper：媒体模块的数据访问文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：MediaReferenceEntity。
 * 阅读入口：insertReference（写入引用）；countByMediaAssetId（统计依据媒体资产标识）；selectByMediaAssetId（按媒体资产标识读取查询）；deleteReference（删除引用）。
 */

package com.starrainnotes.media.mapper;

import com.starrainnotes.media.entity.MediaReferenceEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 媒体引用数据映射，属于媒体模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
@Mapper
public interface MediaReferenceMapper {

    /**
     * 写入引用。
     *
     * 持久化操作：新增，涉及 <code>sr_media_reference</code>。
     *
     * @param reference 引用；类型为 <code>MediaReferenceEntity</code>
     */
    void insertReference(MediaReferenceEntity reference);

    /**
     * 统计依据媒体资产标识。
     *
     * 持久化操作：读取，涉及 <code>sr_media_reference</code>。
     *
     * @param mediaAssetId 关联的媒体资产标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("mediaAssetId")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long countByMediaAssetId(@Param("mediaAssetId") Long mediaAssetId);

    /**
     * 按媒体资产标识读取查询。
     *
     * 持久化操作：读取，涉及 <code>sr_media_reference</code>。
     *
     * @param mediaAssetId 关联的媒体资产标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("mediaAssetId")</code>
     * @return 本次处理得到的条目集合
     */
    List<MediaReferenceEntity> selectByMediaAssetId(@Param("mediaAssetId") Long mediaAssetId);

    /**
     * 删除引用。
     *
     * 持久化操作：删除，涉及 <code>sr_media_reference</code>。
     *
     * @param mediaAssetId 关联的媒体资产标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("mediaAssetId")</code>
     * @param sourceModule 引用或数据来源所属模块；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("sourceModule")</code>
     * @param sourceType 来源对象的业务类型；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("sourceType")</code>
     * @param sourceId 来源对象的业务标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("sourceId")</code>
     * @param usageCode usage代码；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("usageCode")</code>
     * @return SQL 写入影响的记录行数
     */
    int deleteReference(@Param("mediaAssetId") Long mediaAssetId,
                        @Param("sourceModule") String sourceModule,
                        @Param("sourceType") String sourceType,
                        @Param("sourceId") Long sourceId,
                        @Param("usageCode") String usageCode);

    /**
     * 删除全部依据来源。
     *
     * 持久化操作：删除，涉及 <code>sr_media_reference</code>。
     *
     * @param sourceModule 引用或数据来源所属模块；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("sourceModule")</code>
     * @param sourceType 来源对象的业务类型；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("sourceType")</code>
     * @param sourceId 来源对象的业务标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("sourceId")</code>
     * @return SQL 写入影响的记录行数
     */
    int deleteAllBySource(@Param("sourceModule") String sourceModule,
                          @Param("sourceType") String sourceType,
                          @Param("sourceId") Long sourceId);
}
