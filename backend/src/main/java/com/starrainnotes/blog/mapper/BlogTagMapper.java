package com.starrainnotes.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.starrainnotes.blog.entity.BlogTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BlogTagMapper extends BaseMapper<BlogTag> {
    List<BlogTagWithCountRow> selectAllWithPostCount();

    List<BlogTagWithCountRow> selectPublishedWithPostCount();

    int insertIgnore(@Param("tag") BlogTag tag);

    record BlogTagWithCountRow(Long id, String name, String slug, long postCount) {
    }
}
