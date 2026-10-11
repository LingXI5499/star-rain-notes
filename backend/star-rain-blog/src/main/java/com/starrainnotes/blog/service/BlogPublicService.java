/*
 * BlogPublicService：博客模块的业务契约文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：BlogPublicQueryDTO、BlogArchiveMonthVO、BlogArchiveDayVO、BlogPostPublicDetailVO、BlogPostPublicVO、BlogPublicStatsVO、BlogTagVO。
 * 阅读入口：listPosts（列出文章列表）；postBySlug（按固定链接标识读取文章）；archive（archive处理入口）；topicBySlug（按固定链接标识读取专题）。
 */

package com.starrainnotes.blog.service;

import com.starrainnotes.blog.dto.BlogPublicQueryDTO;
import com.starrainnotes.blog.vo.BlogArchiveMonthVO;
import com.starrainnotes.blog.vo.BlogArchiveDayVO;
import com.starrainnotes.blog.vo.BlogPostPublicDetailVO;
import com.starrainnotes.blog.vo.BlogPostPublicVO;
import com.starrainnotes.blog.vo.BlogPublicStatsVO;
import com.starrainnotes.blog.vo.BlogTagVO;
import com.starrainnotes.blog.vo.BlogTopicDetailVO;
import com.starrainnotes.blog.vo.BlogTopicVO;
import com.starrainnotes.common.result.PageResult;
import java.util.List;

/*
 * BLOG-001 / BLOG-002 / BLOG-010 前台公开读取。
 *
 * 所有查询都在 SQL 条件里写死 status = 'PUBLISHED'：
 * “未发布的文章不可见”不能依赖调用方传对参数，也不能靠 Controller 记得过滤。
 */
/**
 * 博客公开业务服务，属于博客模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface BlogPublicService {

    // BLOG-001 列表：可按 tag / topic 组合筛选，不支持时间维度
    /**
     * 列出文章列表。
     *
     * @param query 查询参数；类型为 <code>BlogPublicQueryDTO</code>
     * @return 类型为 <code>PageResult&lt;BlogPostPublicVO&gt;</code> 的处理结果
     */
    PageResult<BlogPostPublicVO> listPosts(BlogPublicQueryDTO query);

    // BLOG-002 阅读：按 slug 读取，DRAFT 与 WITHDRAWN 一律 404
    /**
     * 按固定链接标识读取文章。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @return 类型为 <code>BlogPostPublicDetailVO</code> 的处理结果
     */
    BlogPostPublicDetailVO postBySlug(String slug);

    // BLOG-010 归档：tag / topic / year / month 可组合
    /**
     * archive处理入口。
     *
     * @param query 查询参数；类型为 <code>BlogPublicQueryDTO</code>
     * @return 类型为 <code>PageResult&lt;BlogPostPublicVO&gt;</code> 的处理结果
     */
    PageResult<BlogPostPublicVO> archive(BlogPublicQueryDTO query);

    /*
     * 专题页：专题本身 + 该专题下的文章分页，顺序是策展顺序。
     * 不可见（不存在 / 已停用 / 没有已发布成员）的专题一律 BLOG_TOPIC_NOT_FOUND。
     */
    /**
     * 按固定链接标识读取专题。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @return 类型为 <code>BlogTopicDetailVO</code> 的处理结果
     */
    BlogTopicDetailVO topicBySlug(String slug, int page, int pageSize);

    // 归档侧栏的月份桶
    /**
     * archiveMonths处理入口。
     * @return 本次处理得到的条目集合
     */
    List<BlogArchiveMonthVO> archiveMonths();

    // 博客首页右栏的写作统计：已发布篇数、正文字符数合计、最早发布时间
    /**
     * 统计字段处理入口。
     * @return 类型为 <code>BlogPublicStatsVO</code> 的处理结果
     */
    BlogPublicStatsVO stats();

    /**
     * archiveDays处理入口。
     *
     * @param year 年份；类型为 <code>Integer</code>
     * @param month 月份；类型为 <code>Integer</code>
     * @return 本次处理得到的条目集合
     */
    List<BlogArchiveDayVO> archiveDays(Integer year, Integer month);

    // 前台筛选项
    /**
     * 列出已发布标签列表。
     * @return 本次处理得到的条目集合
     */
    List<BlogTagVO> listPublishedTags();

    /**
     * 列出已发布专题列表。
     * @return 本次处理得到的条目集合
     */
    List<BlogTopicVO> listPublishedTopics();
}
