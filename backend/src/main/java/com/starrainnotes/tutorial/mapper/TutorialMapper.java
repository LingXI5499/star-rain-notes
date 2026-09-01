package com.starrainnotes.tutorial.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.starrainnotes.tutorial.entity.Tutorial;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TutorialMapper extends BaseMapper<Tutorial> {
}
