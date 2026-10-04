package com.starrainnotes.tutorial.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.starrainnotes.tutorial.content.entity.TutorialCategoryEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TutorialCategoryMapper extends BaseMapper<TutorialCategoryEntity> {
}
