package com.starrainnotes.tutorial.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.starrainnotes.tutorial.content.entity.TutorialEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TutorialMapper extends BaseMapper<TutorialEntity> {

    TutorialEntity byIdForUpdate(@Param("id") Long id);
}
