package com.starrainnotes.portfolio.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.starrainnotes.portfolio.entity.WorkEntity;
import org.apache.ibatis.annotations.Param;

public interface WorkMapper extends BaseMapper<WorkEntity> {

    WorkEntity byIdForUpdate(@Param("id") Long id);
}
