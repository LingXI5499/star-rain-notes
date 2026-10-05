package com.starrainnotes.profile.mapper;

import com.starrainnotes.profile.entity.SocialLinkEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/*
 * 作者外部社交链接。SQL 全部在 mapper/profile/SocialLinkMapper.xml。
 */
public interface SocialLinkMapper {

    int insert(SocialLinkEntity socialLink);

    SocialLinkEntity selectById(@Param("id") Long id);

    List<SocialLinkEntity> listByProfileId(@Param("profileId") Long profileId);

    List<SocialLinkEntity> listEnabledByProfileId(@Param("profileId") Long profileId);

    long countByProfileId(@Param("profileId") Long profileId);

    int updateContent(SocialLinkEntity socialLink);

    int updateSortOrder(SocialLinkEntity socialLink);

    int deleteById(@Param("id") Long id);
}
