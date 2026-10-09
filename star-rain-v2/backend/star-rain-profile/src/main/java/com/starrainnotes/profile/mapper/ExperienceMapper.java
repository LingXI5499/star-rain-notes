package com.starrainnotes.profile.mapper;

import com.starrainnotes.profile.entity.ExperienceEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/*
 * 作者教育/经历。SQL 全部在 mapper/profile/ExperienceMapper.xml。
 *
 * 「保存经历」原先走 updateById，会把读出来的整行连同不可变的 profile_id 一起写回；
 * 迁移后 updateContent 只列这条路径真正可能改动的列，profile_id 不参与 SET。
 */
public interface ExperienceMapper {

    int insert(ExperienceEntity experience);

    ExperienceEntity selectById(@Param("id") Long id);

    /* 后台排序用：该 profile 下的全部条目，不按 status 过滤 */
    List<ExperienceEntity> listByProfileId(@Param("profileId") Long profileId);

    /* 公开/后台展示用：只取 ENABLED */
    List<ExperienceEntity> listEnabledByProfileId(@Param("profileId") Long profileId);

    /* 新增时用来取「当前条目数」当初始排序值 */
    long countByProfileId(@Param("profileId") Long profileId);

    int updateContent(ExperienceEntity experience);

    int updateSortOrder(ExperienceEntity experience);

    int deleteById(@Param("id") Long id);
}
