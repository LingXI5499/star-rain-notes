package com.starrainnotes.tutorial.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.starrainnotes.tutorial.entity.TutorialGroupEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TutorialGroupMapper extends BaseMapper<TutorialGroupEntity> {
}
