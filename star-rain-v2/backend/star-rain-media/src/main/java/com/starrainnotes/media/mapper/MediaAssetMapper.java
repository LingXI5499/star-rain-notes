package com.starrainnotes.media.mapper;

import com.starrainnotes.media.enumeration.MediaType;
import com.starrainnotes.media.entity.MediaAssetEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MediaAssetMapper {

    void insertAsset(MediaAssetEntity asset);

    MediaAssetEntity assetById(@Param("id") Long id);

    // 加行锁读取。attach 与 archive 都先走这里，保证两者不会交错：
    // 避免出现“归档检查通过后立刻被重新引用”，也避免锁顺序不一致导致死锁
    MediaAssetEntity assetByIdForUpdate(@Param("id") Long id);

    long assetPageCount(@Param("keyword") String keyword,
                        @Param("mediaType") String mediaType,
                        @Param("status") String status,
                        @Param("accessLevel") String accessLevel);

    List<MediaAssetEntity> assetPage(@Param("keyword") String keyword,
                                     @Param("mediaType") String mediaType,
                                     @Param("status") String status,
                                     @Param("accessLevel") String accessLevel,
                                     @Param("offset") int offset,
                                     @Param("limit") int limit);

    // 只用于重复上传提示，sha256 不作为 MediaAsset 的业务唯一键
    long countBySha256(@Param("sha256") String sha256);

    // 条件更新：只有当前仍是 ACTIVE 才会改成功，返回值可用于判断状态竞态
    int archiveAsset(@Param("id") Long id, @Param("archivedAt") LocalDateTime archivedAt);

    int restoreAsset(@Param("id") Long id);

    int updateAccessLevel(@Param("id") Long id, @Param("accessLevel") String accessLevel);

    // 孤儿文件清理用：数据库中已登记的全部 storageKey
    List<String> allStorageKeys();
}
