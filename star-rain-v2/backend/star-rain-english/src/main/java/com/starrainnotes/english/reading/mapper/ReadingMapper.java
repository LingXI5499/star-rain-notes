package com.starrainnotes.english.reading.mapper;

import com.starrainnotes.english.reading.dto.ReadingDto.Article;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReadingMapper {
    long count(@Param("publicOnly") boolean publicOnly, @Param("search") String search);
    List<Article> list(@Param("publicOnly") boolean publicOnly, @Param("search") String search,
                       @Param("offset") long offset, @Param("limit") int limit);
    Article byId(@Param("id") long id);
    Article publicBySlug(@Param("slug") String slug);
    int insert(Article article);
    int update(Article article);
    int setStatus(@Param("id") long id, @Param("status") String status);
    int delete(@Param("id") long id);
}
