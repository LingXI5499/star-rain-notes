package com.starrainnotes.tutorial.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.starrainnotes.tutorial.entity.TutorialQuestionEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TutorialQuestionMapper extends BaseMapper<TutorialQuestionEntity> {
}
