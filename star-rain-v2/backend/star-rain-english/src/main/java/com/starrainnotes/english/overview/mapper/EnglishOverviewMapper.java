package com.starrainnotes.english.overview.mapper;

import com.starrainnotes.english.overview.entity.EnglishOverviewEntity;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EnglishOverviewMapper extends BaseMapper<EnglishOverviewEntity> {
    int updateContent(@Param("title") String title, @Param("subtitle") String subtitle,
                      @Param("introduction") String introduction,
                      @Param("roadmapMarkdown") String roadmapMarkdown);
}
