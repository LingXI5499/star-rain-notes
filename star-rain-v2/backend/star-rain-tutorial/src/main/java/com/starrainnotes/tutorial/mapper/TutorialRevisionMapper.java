package com.starrainnotes.tutorial.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.starrainnotes.tutorial.entity.TutorialRevisionEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TutorialRevisionMapper extends BaseMapper<TutorialRevisionEntity> {
}
