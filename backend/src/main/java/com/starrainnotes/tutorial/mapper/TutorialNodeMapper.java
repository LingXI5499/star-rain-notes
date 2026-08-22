package com.starrainnotes.tutorial.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.starrainnotes.tutorial.entity.TutorialNode;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TutorialNodeMapper extends BaseMapper<TutorialNode> {
}
