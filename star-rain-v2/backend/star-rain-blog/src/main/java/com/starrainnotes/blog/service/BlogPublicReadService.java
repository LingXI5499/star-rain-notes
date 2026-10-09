package com.starrainnotes.blog.service;

import com.starrainnotes.blog.vo.BlogPostPublicDetailVO;

// 博客公开阅读入口：读取文章详情并记录浏览事件。
public interface BlogPublicReadService {

    BlogPostPublicDetailVO read(String slug);
}
