package com.starrainnotes.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.starrainnotes.blog.entity.BlogPost;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BlogPostMapper extends BaseMapper<BlogPost> {
}
