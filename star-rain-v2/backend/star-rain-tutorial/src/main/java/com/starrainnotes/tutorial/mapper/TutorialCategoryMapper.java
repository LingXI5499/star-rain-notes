package com.starrainnotes.tutorial.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.starrainnotes.tutorial.entity.TutorialCategoryEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TutorialCategoryMapper extends BaseMapper<TutorialCategoryEntity> {
}
