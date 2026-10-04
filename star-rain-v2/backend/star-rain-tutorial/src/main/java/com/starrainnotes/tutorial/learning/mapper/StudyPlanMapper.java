package com.starrainnotes.tutorial.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.starrainnotes.tutorial.learning.entity.StudyPlanEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StudyPlanMapper extends BaseMapper<StudyPlanEntity> {
}
