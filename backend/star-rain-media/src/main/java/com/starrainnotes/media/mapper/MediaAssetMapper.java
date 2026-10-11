/*
 * MediaAssetMapper：媒体模块的数据访问文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：MediaType、MediaAssetEntity。
 * 阅读入口：insertAsset（写入资产）；assetById（按标识读取资产）；assetByIdForUpdate（按标识加行锁读取资产）；assetPageCount（资产分页数量处理入口）。
 */

package com.starrainnotes.media.mapper;

import com.starrainnotes.media.enumeration.MediaType;
import com.starrainnotes.media.entity.MediaAssetEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 媒体资产数据映射，属于媒体模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
@Mapper
public interface MediaAssetMapper {

    /**
     * 写入资产。
     *
     * 持久化操作：新增，涉及 <code>sr_media_asset</code>。
     *
     * @param asset 资产；类型为 <code>MediaAssetEntity</code>
     */
    void insertAsset(MediaAssetEntity asset);

    /**
     * 按标识读取资产。
     *
     * 持久化操作：读取，涉及 <code>sr_media_asset</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>MediaAssetEntity</code> 的处理结果
     */
    MediaAssetEntity assetById(@Param("id") Long id);

    // 加行锁读取。attach 与 archive 都先走这里，保证两者不会交错：
    // 避免出现“归档检查通过后立刻被重新引用”，也避免锁顺序不一致导致死锁
    /**
     * 按标识加行锁读取资产。
     *
     * 持久化操作：读取，涉及 <code>sr_media_asset</code>。
     * 此查询使用行锁；调用方应在事务中完成后续状态检查和写入。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>MediaAssetEntity</code> 的处理结果
     */
    MediaAssetEntity assetByIdForUpdate(@Param("id") Long id);

    /**
     * 资产分页数量处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_media_asset</code>。
     *
     * @param keyword 关键词；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("keyword")</code>
     * @param mediaType 媒体类型；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("mediaType")</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("status")</code>
     * @param accessLevel 媒体访问级别；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("accessLevel")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long assetPageCount(@Param("keyword") String keyword,
                        @Param("mediaType") String mediaType,
                        @Param("status") String status,
                        @Param("accessLevel") String accessLevel);

    /**
     * 资产分页处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_media_asset</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param keyword 关键词；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("keyword")</code>
     * @param mediaType 媒体类型；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("mediaType")</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("status")</code>
     * @param accessLevel 媒体访问级别；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("accessLevel")</code>
     * @param offset 查询跳过的记录数；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("offset")</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("limit")</code>
     * @return 本次处理得到的条目集合
     */
    List<MediaAssetEntity> assetPage(@Param("keyword") String keyword,
                                     @Param("mediaType") String mediaType,
                                     @Param("status") String status,
                                     @Param("accessLevel") String accessLevel,
                                     @Param("offset") int offset,
                                     @Param("limit") int limit);

    // 只用于重复上传提示，sha256 不作为 MediaAsset 的业务唯一键
    /**
     * 统计依据摘要256。
     *
     * 持久化操作：读取，涉及 <code>sr_media_asset</code>。
     *
     * @param sha256 文件内容的 SHA-256 摘要；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("sha256")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long countBySha256(@Param("sha256") String sha256);

    // 条件更新：只有当前仍是 ACTIVE 才会改成功，返回值可用于判断状态竞态
    /**
     * archive资产处理入口。
     *
     * 持久化操作：更新，涉及 <code>sr_media_asset</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param archivedAt 已归档时间；类型为 <code>LocalDateTime</code>；对应 MyBatis 命名参数 <code>@Param("archivedAt")</code>
     * @return SQL 写入影响的记录行数
     */
    int archiveAsset(@Param("id") Long id, @Param("archivedAt") LocalDateTime archivedAt);

    /**
     * 恢复资产。
     *
     * 持久化操作：更新，涉及 <code>sr_media_asset</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return SQL 写入影响的记录行数
     */
    int restoreAsset(@Param("id") Long id);

    /**
     * 更新访问Level。
     *
     * 持久化操作：更新，涉及 <code>sr_media_asset</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param accessLevel 媒体访问级别；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("accessLevel")</code>
     * @return SQL 写入影响的记录行数
     */
    int updateAccessLevel(@Param("id") Long id, @Param("accessLevel") String accessLevel);

    // 孤儿文件清理用：数据库中已登记的全部 storageKey
    /**
     * 全部存储Keys处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_media_asset</code>。
     * @return 本次处理得到的条目集合
     */
    List<String> allStorageKeys();
}
