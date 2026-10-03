package com.starrainnotes.blog.service;

import com.starrainnotes.blog.dto.BlogPublicQueryDTO;
import com.starrainnotes.blog.vo.BlogArchiveMonthVO;
import com.starrainnotes.blog.vo.BlogArchiveDayVO;
import com.starrainnotes.blog.vo.BlogPostPublicDetailVO;
import com.starrainnotes.blog.vo.BlogPostPublicVO;
import com.starrainnotes.blog.vo.BlogTagVO;
import com.starrainnotes.blog.vo.BlogTopicVO;
import com.starrainnotes.common.result.PageResult;
import java.util.List;

/*
 * BLOG-001 / BLOG-002 / BLOG-010 前台公开读取。
 *
 * 所有查询都在 SQL 条件里写死 status = 'PUBLISHED'：
 * “未发布的文章不可见”不能依赖调用方传对参数，也不能靠 Controller 记得过滤。
 */
public interface BlogPublicService {

    // BLOG-001 列表：可按 tag / topic 组合筛选，不支持时间维度
    PageResult<BlogPostPublicVO> listPosts(BlogPublicQueryDTO query);

    // BLOG-002 阅读：按 slug 读取，DRAFT 与 WITHDRAWN 一律 404
    BlogPostPublicDetailVO postBySlug(String slug);

    // BLOG-010 归档：tag / topic / year / month 可组合
    PageResult<BlogPostPublicVO> archive(BlogPublicQueryDTO query);

    // 归档侧栏的月份桶
    List<BlogArchiveMonthVO> archiveMonths();

    List<BlogArchiveDayVO> archiveDays(Integer year, Integer month);

    // 前台筛选项
    List<BlogTagVO> listPublishedTags();

    List<BlogTopicVO> listPublishedTopics();
}
