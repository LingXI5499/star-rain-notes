package com.starrainnotes.english.writing.mapper;

import com.starrainnotes.english.writing.dto.WritingResourceDto.Resource;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WritingResourceMapper {
    long count(@Param("publicOnly") boolean publicOnly, @Param("search") String search);
    List<Resource> list(@Param("publicOnly") boolean publicOnly, @Param("search") String search,
                        @Param("offset") long offset, @Param("limit") int limit);
    Resource byId(@Param("id") long id);
    Resource publicBySlug(@Param("slug") String slug);
    int insert(Resource resource);
    int update(Resource resource);
    int setStatus(@Param("id") long id, @Param("status") String status);
    int delete(@Param("id") long id);
}
