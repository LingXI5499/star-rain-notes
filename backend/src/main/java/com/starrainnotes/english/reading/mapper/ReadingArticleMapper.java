package com.starrainnotes.english.reading.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.starrainnotes.english.reading.entity.ReadingArticle;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReadingArticleMapper extends BaseMapper<ReadingArticle> {
}
