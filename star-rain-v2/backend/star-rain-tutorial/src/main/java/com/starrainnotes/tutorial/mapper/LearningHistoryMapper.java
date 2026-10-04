package com.starrainnotes.tutorial.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.starrainnotes.tutorial.entity.LearningHistoryEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LearningHistoryMapper extends BaseMapper<LearningHistoryEntity> {
}
