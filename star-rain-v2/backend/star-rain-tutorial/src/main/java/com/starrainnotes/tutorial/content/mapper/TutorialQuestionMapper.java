package com.starrainnotes.tutorial.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.starrainnotes.tutorial.content.entity.TutorialQuestionEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TutorialQuestionMapper extends BaseMapper<TutorialQuestionEntity> {
}
