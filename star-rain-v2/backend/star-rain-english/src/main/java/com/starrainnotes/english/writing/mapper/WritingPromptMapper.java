package com.starrainnotes.english.writing.mapper;

import com.starrainnotes.english.writing.dto.WritingPromptDto.Prompt;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WritingPromptMapper {
    long count(@Param("publicOnly") boolean publicOnly, @Param("search") String search);
    List<Prompt> list(@Param("publicOnly") boolean publicOnly, @Param("search") String search,
                      @Param("offset") long offset, @Param("limit") int limit);
    Prompt byId(@Param("id") long id);
    Prompt publicBySlug(@Param("slug") String slug);
    int insert(Prompt prompt);
    int update(Prompt prompt);
    int setStatus(@Param("id") long id, @Param("status") String status);
    int delete(@Param("id") long id);
}
