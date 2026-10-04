package com.starrainnotes.tutorial.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.starrainnotes.tutorial.content.entity.TutorialEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TutorialMapper extends BaseMapper<TutorialEntity> {
    @Select("SELECT id, category_id, slug, title, summary, sort_order, cover_media_asset_id, "
            + "publication_status, editing_status, published_revision_id, published_at, withdrawn_at, "
            + "created_by_account_id, updated_by_account_id, created_at, updated_at "
            + "FROM sr_tutorial WHERE id = #{id} FOR UPDATE")
    TutorialEntity byIdForUpdate(@Param("id") Long id);
}
