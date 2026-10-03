package com.starrainnotes.tutorial.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.starrainnotes.tutorial.entity.TutorialChapterEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TutorialChapterMapper extends BaseMapper<TutorialChapterEntity> {
}
