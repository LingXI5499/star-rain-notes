package com.starrainnotes.portfolio.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.starrainnotes.portfolio.entity.WorkEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface WorkMapper extends BaseMapper<WorkEntity> {
    @Select("SELECT id, slug, work_type, title, summary, body_markdown, status, published_at, withdrawn_at, "
            + "created_by_account_id, updated_by_account_id, created_at, updated_at "
            + "FROM sr_portfolio_work WHERE id = #{id} FOR UPDATE")
    WorkEntity byIdForUpdate(@Param("id") Long id);
}
