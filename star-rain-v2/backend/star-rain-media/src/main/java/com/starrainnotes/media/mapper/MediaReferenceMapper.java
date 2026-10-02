package com.starrainnotes.media.mapper;

import com.starrainnotes.media.entity.MediaReferenceEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MediaReferenceMapper {

    void insertReference(MediaReferenceEntity reference);

    long countByMediaAssetId(@Param("mediaAssetId") Long mediaAssetId);

    List<MediaReferenceEntity> selectByMediaAssetId(@Param("mediaAssetId") Long mediaAssetId);

    int deleteReference(@Param("mediaAssetId") Long mediaAssetId,
                        @Param("sourceModule") String sourceModule,
                        @Param("sourceType") String sourceType,
                        @Param("sourceId") Long sourceId,
                        @Param("usageCode") String usageCode);

    int deleteAllBySource(@Param("sourceModule") String sourceModule,
                          @Param("sourceType") String sourceType,
                          @Param("sourceId") Long sourceId);
}
