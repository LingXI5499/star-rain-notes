package com.starrainnotes.english.overview;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface EnglishOverviewMapper extends BaseMapper<EnglishOverviewEntity> {
    @Update("""
            UPDATE sr_english_overview SET title = #{title}, subtitle = #{subtitle},
                introduction = #{introduction}, roadmap_markdown = #{roadmapMarkdown},
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE id = 1
            """)
    int updateContent(@Param("title") String title, @Param("subtitle") String subtitle,
                      @Param("introduction") String introduction,
                      @Param("roadmapMarkdown") String roadmapMarkdown);
}
