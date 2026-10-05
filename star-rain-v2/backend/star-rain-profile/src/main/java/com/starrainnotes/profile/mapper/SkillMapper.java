package com.starrainnotes.profile.mapper;

import com.starrainnotes.profile.entity.SkillEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/*
 * 作者技能 / 方向 / 兴趣。SQL 全部在 mapper/profile/SkillMapper.xml。
 *
 * description / proficiency 原先带 @TableField(updateStrategy = FieldStrategy.ALWAYS)，
 * 「清空说明」必须能把列写成 NULL，这套语义由 SkillMapper.xml 的 updateContent 承担。
 */
public interface SkillMapper {

    int insert(SkillEntity skill);

    SkillEntity selectById(@Param("id") Long id);

    List<SkillEntity> listByProfileId(@Param("profileId") Long profileId);

    List<SkillEntity> listEnabledByProfileId(@Param("profileId") Long profileId);

    long countByProfileId(@Param("profileId") Long profileId);

    int updateContent(SkillEntity skill);

    int updateSortOrder(SkillEntity skill);

    int deleteById(@Param("id") Long id);
}
