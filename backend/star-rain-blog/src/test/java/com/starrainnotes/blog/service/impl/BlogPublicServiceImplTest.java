package com.starrainnotes.blog.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.starrainnotes.blog.dto.BlogPublicQueryDTO;
import com.starrainnotes.blog.entity.BlogPostEntity;
import com.starrainnotes.blog.mapper.BlogPostMapper;
import com.starrainnotes.blog.mapper.BlogTagMapper;
import com.starrainnotes.blog.mapper.BlogTopicMapper;
import com.starrainnotes.blog.service.BlogViewAssembler;
import com.starrainnotes.blog.vo.BlogArchiveMonthVO;
import com.starrainnotes.blog.vo.BlogArchiveDayVO;
import com.starrainnotes.blog.vo.BlogPostPublicDetailVO;
import com.starrainnotes.blog.vo.BlogPostNeighborVO;
import com.starrainnotes.blog.vo.BlogPostPublicVO;
import com.starrainnotes.blog.vo.BlogTagVO;
import com.starrainnotes.blog.vo.BlogTopicVO;
import com.starrainnotes.common.exception.ApiException;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/*
 * BLOG-001 / BLOG-002 / BLOG-010 公开读取测试。
 *
 * “只返回 PUBLISHED”靠 SQL 常量保证，单测无法直接验证 SQL，因此这里做两件事：
 * 1. 断言公开读取只走 publishedPostBySlug / publishedPage，绝不碰不过滤状态的 postById；
 * 2. 断言时间归档被换算成 [from, to) 半开区间，跨年边界不出错。
 */
@ExtendWith(MockitoExtension.class)
class BlogPublicServiceImplTest {

    @Mock
    private BlogPostMapper postMapper;

    @Mock
    private BlogTagMapper tagMapper;

    @Mock
    private BlogTopicMapper topicMapper;

    @Mock
    private BlogViewAssembler assembler;

    @InjectMocks
    private BlogPublicServiceImpl service;

    // ------------------------------------------------------------------
    // BLOG-001 列表
    // ------------------------------------------------------------------

    @Test
    @DisplayName("列表分页非法报 BLOG_QUERY_INVALID")
    void listRejectsInvalidPaging() {
        BlogPublicQueryDTO query = new BlogPublicQueryDTO();
        query.setPage(0);

        assertThatThrownBy(() -> service.listPosts(query))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_QUERY_INVALID");
        verify(postMapper, never()).publishedPageCount(any(), any(), any(), any());
    }

    @Test
    @DisplayName("列表筛选 slug 非法报 BLOG_QUERY_INVALID，而不是静默返回空列表")
    void listRejectsInvalidFilterSlug() {
        BlogPublicQueryDTO query = new BlogPublicQueryDTO();
        query.setTag("不合法 slug");

        assertThatThrownBy(() -> service.listPosts(query))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_QUERY_INVALID");
    }

    @Test
    @DisplayName("列表按 tag / topic 组合筛选，slug 规范化后传给持久层")
    void listPassesNormalizedFilters() {
        BlogPublicQueryDTO query = new BlogPublicQueryDTO();
        query.setTag("  Java  ");
        query.setTopic("Roadmap");
        when(postMapper.publishedPageCount(List.of("java"), "roadmap", null, null)).thenReturn(1L);
        when(postMapper.publishedPage(List.of("java"), "roadmap", null, null, 0, 20)).thenReturn(List.of(post(9L)));
        when(assembler.toPublicVOs(any())).thenReturn(List.of(BlogPostPublicVO.builder().id(9L).build()));

        var result = service.listPosts(query);

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getItems()).hasSize(1);
        // 公开读取绝不使用不过滤状态的方法
        verify(postMapper, never()).postById(anyLong());
    }

    @Test
    @DisplayName("多标签 ?tags=a,b：逗号拆成集合、去重、按「命中任一」交给 SQL")
    void listPassesMultipleTagSlugs() {
        BlogPublicQueryDTO query = new BlogPublicQueryDTO();
        query.setTags(" Java , spring-boot ,java,");

        when(postMapper.publishedPageCount(List.of("java", "spring-boot"), null, null, null)).thenReturn(0L);

        assertThat(service.listPosts(query).getItems()).isEmpty();
        // 首个参数是去重后的集合：重复的 java 只出现一次，空片段被丢掉
        verify(postMapper).publishedPageCount(List.of("java", "spring-boot"), null, null, null);
    }

    @Test
    @DisplayName("单标签 tag 与多标签 tags 同时给出时合并去重，不报错")
    void listMergesSingleAndMultipleTags() {
        BlogPublicQueryDTO query = new BlogPublicQueryDTO();
        query.setTag("java");
        query.setTags("spring-boot,java");

        when(postMapper.publishedPageCount(List.of("java", "spring-boot"), null, null, null)).thenReturn(0L);

        service.listPosts(query);

        verify(postMapper).publishedPageCount(List.of("java", "spring-boot"), null, null, null);
    }

    @Test
    @DisplayName("多标签里有一个非法 slug 同样报 BLOG_QUERY_INVALID")
    void listRejectsInvalidSlugInsideTags() {
        BlogPublicQueryDTO query = new BlogPublicQueryDTO();
        query.setTags("java,不合法 slug");

        assertThatThrownBy(() -> service.listPosts(query))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_QUERY_INVALID");
    }

    @Test
    @DisplayName("空结果不触发第二次查询")
    void listSkipsSecondQueryWhenEmpty() {
        BlogPublicQueryDTO query = new BlogPublicQueryDTO();
        when(postMapper.publishedPageCount(List.of(), null, null, null)).thenReturn(0L);

        var result = service.listPosts(query);

        assertThat(result.getItems()).isEmpty();
        verify(postMapper, never()).publishedPage(any(), any(), any(), any(), eq(0), eq(20));
    }

    // ------------------------------------------------------------------
    // BLOG-002 阅读
    // ------------------------------------------------------------------

    @Test
    @DisplayName("阅读文章：slug 规范化后只查已发布记录")
    void postBySlugUsesPublishedQuery() {
        BlogPostEntity post = post(9L);
        when(postMapper.publishedPostBySlug("first-post")).thenReturn(post);
        when(assembler.toPublicDetailVO(post))
                .thenReturn(BlogPostPublicDetailVO.builder().id(9L).slug("first-post").build());

        BlogPostPublicDetailVO result = service.postBySlug("First-Post");

        assertThat(result.getSlug()).isEqualTo("first-post");
        verify(postMapper, never()).postById(anyLong());
    }

    @Test
    @DisplayName("相邻导航只取已发布文章，按发布时间和 ID 定位")
    void postBySlugAddsPublishedNeighbors() {
        BlogPostEntity post = post(9L);
        BlogPostNeighborVO previous = BlogPostNeighborVO.builder().slug("older").title("上一篇").build();
        BlogPostNeighborVO next = BlogPostNeighborVO.builder().slug("newer").title("下一篇").build();
        when(postMapper.publishedPostBySlug("first-post")).thenReturn(post);
        when(assembler.toPublicDetailVO(post)).thenReturn(BlogPostPublicDetailVO.builder().id(9L).build());
        when(postMapper.publishedPrevious(post.getPublishedAt(), 9L)).thenReturn(previous);
        when(postMapper.publishedNext(post.getPublishedAt(), 9L)).thenReturn(next);

        BlogPostPublicDetailVO result = service.postBySlug("first-post");

        assertThat(result.getPrevious()).isEqualTo(previous);
        assertThat(result.getNext()).isEqualTo(next);
    }

    @Test
    @DisplayName("草稿与已撤回文章对外一律 404：不区分“不存在”和“未公开”")
    void postBySlugHidesUnpublished() {
        when(postMapper.publishedPostBySlug("draft-post")).thenReturn(null);

        assertThatThrownBy(() -> service.postBySlug("draft-post"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_POST_NOT_FOUND");
    }

    @Test
    @DisplayName("slug 为空白直接 404，不去查库")
    void postBySlugRejectsBlank() {
        assertThatThrownBy(() -> service.postBySlug("   "))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_POST_NOT_FOUND");
        verify(postMapper, never()).publishedPostBySlug(any());
    }

    // ------------------------------------------------------------------
    // BLOG-010 归档
    // ------------------------------------------------------------------

    @Test
    @DisplayName("按月归档换算成半开区间：7 月 = [7/1 00:00, 8/1 00:00)")
    void archiveMonthComputesHalfOpenRange() {
        BlogPublicQueryDTO query = new BlogPublicQueryDTO();
        query.setYear(2026);
        query.setMonth(7);
        LocalDateTime from = LocalDateTime.of(2026, 7, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 8, 1, 0, 0);
        when(postMapper.publishedPageCount(List.of(), null, from, to)).thenReturn(1L);
        when(postMapper.publishedPage(List.of(), null, from, to, 0, 20)).thenReturn(List.of(post(9L)));
        when(assembler.toPublicVOs(any())).thenReturn(List.of(BlogPostPublicVO.builder().id(9L).build()));

        var result = service.archive(query);

        assertThat(result.getTotal()).isEqualTo(1L);
    }

    @Test
    @DisplayName("按年归档跨年边界正确：2026 = [2026-01-01, 2027-01-01)")
    void archiveYearComputesHalfOpenRange() {
        BlogPublicQueryDTO query = new BlogPublicQueryDTO();
        query.setYear(2026);
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2027, 1, 1, 0, 0);
        when(postMapper.publishedPageCount(eq(List.of()), isNull(), eq(from), eq(to))).thenReturn(0L);

        assertThat(service.archive(query).getItems()).isEmpty();
    }

    @Test
    @DisplayName("12 月归档跨年到次年 1 月，而不是 13 月")
    void archiveDecemberRollsOver() {
        BlogPublicQueryDTO query = new BlogPublicQueryDTO();
        query.setYear(2026);
        query.setMonth(12);
        LocalDateTime from = LocalDateTime.of(2026, 12, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2027, 1, 1, 0, 0);
        when(postMapper.publishedPageCount(eq(List.of()), isNull(), eq(from), eq(to))).thenReturn(0L);

        assertThat(service.archive(query).getItems()).isEmpty();
    }

    @Test
    @DisplayName("按天归档只查询当日的半开时间范围")
    void archiveDayComputesHalfOpenRange() {
        BlogPublicQueryDTO query = new BlogPublicQueryDTO();
        query.setYear(2026);
        query.setMonth(2);
        query.setDay(28);
        LocalDateTime from = LocalDateTime.of(2026, 2, 28, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 3, 1, 0, 0);
        when(postMapper.publishedPageCount(List.of(), null, from, to)).thenReturn(0L);

        assertThat(service.archive(query).getItems()).isEmpty();
    }

    @Test
    @DisplayName("指定月份的日历只读取该月的发布日桶")
    void archiveDaysQueriesMonth() {
        LocalDateTime from = LocalDateTime.of(2026, 2, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 3, 1, 0, 0);
        when(postMapper.archiveDays(from, to)).thenReturn(List.of(
                BlogArchiveDayVO.builder().day(28).postCount(2L).build()));

        assertThat(service.archiveDays(2026, 2)).extracting(BlogArchiveDayVO::getDay).containsExactly(28);
        assertThatThrownBy(() -> service.archiveDays(2026, null))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_QUERY_INVALID");
    }

    @Test
    @DisplayName("只给 month 不给 year 报 BLOG_QUERY_INVALID")
    void archiveRejectsMonthWithoutYear() {
        BlogPublicQueryDTO query = new BlogPublicQueryDTO();
        query.setMonth(7);

        assertThatThrownBy(() -> service.archive(query))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_QUERY_INVALID");
    }

    @Test
    @DisplayName("归档年份与月份越界报 BLOG_QUERY_INVALID")
    void archiveRejectsOutOfRangeTime() {
        BlogPublicQueryDTO badYear = new BlogPublicQueryDTO();
        badYear.setYear(1999);
        assertThatThrownBy(() -> service.archive(badYear))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_QUERY_INVALID");

        BlogPublicQueryDTO badMonth = new BlogPublicQueryDTO();
        badMonth.setYear(2026);
        badMonth.setMonth(13);
        assertThatThrownBy(() -> service.archive(badMonth))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("BLOG_QUERY_INVALID");
    }

    @Test
    @DisplayName("归档可同时带 tag / topic / 时间条件")
    void archiveCombinesFilters() {
        BlogPublicQueryDTO query = new BlogPublicQueryDTO();
        query.setTag("java");
        query.setTopic("roadmap");
        query.setYear(2026);
        query.setMonth(7);
        LocalDateTime from = LocalDateTime.of(2026, 7, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 8, 1, 0, 0);
        when(postMapper.publishedPageCount(List.of("java"), "roadmap", from, to)).thenReturn(0L);

        assertThat(service.archive(query).getItems()).isEmpty();
        verify(postMapper, never()).postById(anyLong());
    }

    // ------------------------------------------------------------------
    // 归档月份与前台筛选项
    // ------------------------------------------------------------------

    @Test
    @DisplayName("归档月份桶与前台筛选项直接取已发布口径的数据")
    void delegatedReads() {
        when(postMapper.archiveMonths()).thenReturn(List.of(
                BlogArchiveMonthVO.builder().year(2026).month(7).postCount(3L).build()));
        when(tagMapper.publishedTags()).thenReturn(List.of(BlogTagVO.builder().id(5L).build()));
        when(topicMapper.publishedTopics()).thenReturn(List.of(BlogTopicVO.builder().id(3L).build()));

        assertThat(service.archiveMonths()).hasSize(1);
        assertThat(service.archiveMonths().get(0).getPostCount()).isEqualTo(3L);
        assertThat(service.listPublishedTags()).hasSize(1);
        assertThat(service.listPublishedTopics()).hasSize(1);
    }

    private static BlogPostEntity post(Long id) {
        BlogPostEntity entity = new BlogPostEntity();
        entity.setId(id);
        entity.setSlug("first-post");
        entity.setTitle("第一篇");
        entity.setStatus("PUBLISHED");
        entity.setPublishedAt(LocalDateTime.of(2026, 7, 15, 10, 0));
        return entity;
    }
}
