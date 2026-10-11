package com.starrainnotes.profile.mapper;

import com.starrainnotes.profile.entity.FeaturedContentEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/*
 * 作者精选内容引用。SQL 全部在 mapper/profile/FeaturedContentMapper.xml。
 *
 * 这张表的业务只有「新增一条引用」「删除」「调整顺序」：content_type / content_id 是引用的身份，
 * title_override 只在新增时写入，因此没有 updateContent，只有 updateSortOrder。
 */
public interface FeaturedContentMapper {

    int insert(FeaturedContentEntity featuredContent);

    FeaturedContentEntity selectById(@Param("id") Long id);

    List<FeaturedContentEntity> listByProfileId(@Param("profileId") Long profileId);

    List<FeaturedContentEntity> listEnabledByProfileId(@Param("profileId") Long profileId);

    long countByProfileId(@Param("profileId") Long profileId);

    int updateSortOrder(FeaturedContentEntity featuredContent);

    int deleteById(@Param("id") Long id);
}
