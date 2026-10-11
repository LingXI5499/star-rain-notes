package com.starrainnotes.english.overview.mapper;

import com.starrainnotes.english.overview.entity.EnglishOverviewEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/*
 * 英语概览（全表单行，CHECK (id = 1)）。SQL 全部在 mapper/english/EnglishOverviewMapper.xml。
 *
 * 不再继承 MyBatis-Plus 的 BaseMapper：这张表只有「读唯一那行」和「改文案」两种访问形状，
 * 原先的 selectById(1) 由框架运行期拼装，源码里看不到实际 SQL。
 */
@Mapper
public interface EnglishOverviewMapper {

    EnglishOverviewEntity selectSingleton();

    int updateContent(@Param("title") String title, @Param("subtitle") String subtitle,
                      @Param("introduction") String introduction,
                      @Param("roadmapMarkdown") String roadmapMarkdown);
}
