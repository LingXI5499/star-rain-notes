package com.starrainnotes.tutorial.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.starrainnotes.tutorial.learning.entity.LearningHistoryEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LearningHistoryMapper extends BaseMapper<LearningHistoryEntity> {
}
