package com.starrainnotes.boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.starrainnotes.blog.dto.BlogPostTagRow;
import com.starrainnotes.blog.dto.BlogPostTopicRow;
import com.starrainnotes.blog.dto.BlogTopicMemberRow;
import com.starrainnotes.blog.dto.BlogTopicOrderItem;
import com.starrainnotes.blog.entity.BlogPostEntity;
import com.starrainnotes.blog.entity.BlogTagEntity;
import com.starrainnotes.blog.entity.BlogTopicEntity;
import com.starrainnotes.blog.mapper.BlogPostMapper;
import com.starrainnotes.blog.mapper.BlogTagMapper;
import com.starrainnotes.blog.mapper.BlogTopicMapper;
import com.starrainnotes.blog.vo.BlogArchiveDayVO;
import com.starrainnotes.blog.vo.BlogArchiveMonthVO;
import com.starrainnotes.blog.vo.BlogPostNeighborVO;
import com.starrainnotes.blog.vo.BlogTagVO;
import com.starrainnotes.blog.vo.BlogTopicVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/*
 * blog 模块 3 个 Mapper（BlogPostMapper、BlogTagMapper、BlogTopicMapper）的真实数据库读写验证。
 *
 * 这 30 多条语句里最容易「编译通过、运行期才错」的有三类，本类逐个钉住：
 *   1. **公开读取的状态常量写死在 SQL 里**（publishedFilters 的 p.status = 'PUBLISHED'、
 *      publishedTopicPage 的 t.status = 'ENABLED'）：草稿、已撤回文章、停用专题必须进不了公开结果；
 *   2. **返回 VO/Row 的列别名**：post_count / member_count / post_id / topic_id 写错只会静默变成
 *      null 或 0，没有任何类型检查能发现；
 *   3. **状态守卫**：publishPost 只在 DRAFT/WITHDRAWN 生效、withdrawPost 只在 PUBLISHED 生效、
 *      deletePost 删不掉 PUBLISHED，且 publishPost 用 COALESCE 保住首次发布时间。
 *
 * 相邻文章（publishedPrevious/Next）与归档月份（archiveMonths）是全库查询，没法用关键字隔离，
 * 所以探针文章统一用 2099 年的发布时间，落到真实数据之外的时间窗里，断言才稳定。
 *
 * 全程在一个不 commit 的 SqlSession 里跑，@AfterEach 回滚；最后一个用例用新连接复核。
 */
class BlogMapperXmlTest extends MapperXmlIntegrationSupport {

    private static final AtomicLong PROBE = new AtomicLong(System.nanoTime() % 1_000_000L);
    private static final LocalDateTime PROBE_BASE = LocalDateTime.of(2099, 1, 1, 0, 0);

    private SqlSession session;
    private BlogPostMapper posts;
    private BlogTagMapper tags;
    private BlogTopicMapper topics;

    @BeforeEach
    void open() {
        session = openSession();
        posts = session.getMapper(BlogPostMapper.class);
        tags = session.getMapper(BlogTagMapper.class);
        topics = session.getMapper(BlogTopicMapper.class);
    }

    @AfterEach
    void rollback() {
        if (session != null) {
            session.rollback();
            session.close();
        }
    }

    @Test
    void everyBlogMapperIsReachableThroughTheSession() {
        assertNotNull(posts);
        assertNotNull(tags);
        assertNotNull(topics);
    }

    @Test
    void insertedPostRoundTripsEveryColumnAndOnlyPublishedIsPubliclyVisible() {
        String slug = unique("mapperxmlpost");
        BlogPostEntity post = newPost(slug, unique("mapperxmlposttitle"), "DRAFT", null);
        posts.insertPost(post);
        assertNotNull(post.getId(), "insertPost 没有把自增主键回填到实体上");

        BlogPostEntity loaded = posts.postById(post.getId());
        assertNotNull(loaded, "postById 读不到刚插入的行");
        assertEquals(slug, loaded.getSlug());
        assertEquals(post.getTitle(), loaded.getTitle());
        assertEquals("XML 验证摘要", loaded.getSummary());
        assertEquals("# XML 验证正文", loaded.getBodyMarkdown());
        assertNull(loaded.getCoverMediaAssetId());
        assertEquals("DRAFT", loaded.getStatus());
        assertNull(loaded.getPublishedAt());
        assertNull(loaded.getWithdrawnAt());
        assertEquals(post.getCreatedByAccountId(), loaded.getCreatedByAccountId());
        assertEquals(post.getUpdatedByAccountId(), loaded.getUpdatedByAccountId());
        assertNotNull(loaded.getCreatedAt(), "created_at 由列默认值填充，应能读回");
        assertNotNull(loaded.getUpdatedAt());

        assertEquals(post.getId(), posts.postByIdForUpdate(post.getId()).getId(),
                "FOR UPDATE 读不到刚插入的行");
        assertNull(posts.postById(-1L), "不存在的 id 应返回 null");

        assertEquals(1, posts.countBySlug(slug, null), "slug 查重没生效");
        assertEquals(0, posts.countBySlug(slug, post.getId()), "excludeId 应当把「改自己」排除掉");
        assertEquals(1, posts.countBySlug(slug, probeId()), "excludeId 不该排除别的文章");
        assertEquals(0, posts.countBySlug(unique("mapperxmlpost-missing"), null));

        // 公开读取只认 PUBLISHED：草稿在 SQL 层就被挡住
        assertNull(posts.publishedPostBySlug(slug), "草稿不应出现在公开详情里");
        assertNull(posts.publishedPostById(post.getId()), "草稿不应出现在公开详情里");

        String publishedSlug = unique("mapperxmlpost");
        BlogPostEntity published = newPost(publishedSlug, unique("mapperxmlposttitle"),
                "PUBLISHED", PROBE_BASE);
        posts.insertPost(published);
        assertNotNull(posts.publishedPostBySlug(publishedSlug), "已发布文章应当能按 slug 公开读取");
        assertNotNull(posts.publishedPostById(published.getId()), "已发布文章应当能按 id 公开读取");
        assertEquals(1, posts.countBySlug(publishedSlug, probeId()), "excludeId 不该排除别的文章");
    }

    @Test
    void adminPageFiltersByKeywordStatusTagAndTopic() {
        String keyword = unique("mapperxmladminpage");
        BlogPostEntity first = newPost(unique("mapperxmlpost"), keyword + "-first", "DRAFT", null);
        posts.insertPost(first);
        BlogPostEntity second = newPost(unique("mapperxmlpost"), keyword + "-second", "PUBLISHED", PROBE_BASE);
        second.setUpdatedByAccountId(probeId());
        posts.insertPost(second);
        BlogPostEntity ignored = newPost(unique("mapperxmlpost"), unique("mapperxmlothertitle"), "DRAFT", null);
        posts.insertPost(ignored);

        BlogTagEntity tag = newTag(unique("mapperxmltag"), unique("mapperxmltagname"), "ENABLED");
        tags.insertTag(tag);
        BlogTopicEntity topic = newTopic(unique("mapperxmltopic"), unique("mapperxmltopicname"), "ENABLED");
        topics.insertTopic(topic);
        assertEquals(1, tags.insertPostTag(second.getId(), tag.getId()));
        assertEquals(1, topics.insertTopicPost(topic.getId(), second.getId(), 5));

        assertEquals(2, posts.adminPageCount(keyword, null, null, null), "keyword 过滤（标题）没生效");
        assertEquals(1, posts.adminPageCount(keyword, "DRAFT", null, null), "status 过滤没生效");
        assertEquals(1, posts.adminPageCount(keyword, "PUBLISHED", null, null));
        assertEquals(1, posts.adminPageCount(keyword, null, tag.getId(), null), "tagId 过滤没生效");
        assertEquals(1, posts.adminPageCount(keyword, null, null, topic.getId()), "topicId 过滤没生效");
        assertEquals(0, posts.adminPageCount(keyword, null, probeId(), null));
        assertEquals(0, posts.adminPageCount(keyword, null, null, probeId()));
        assertEquals(0, posts.adminPageCount(keyword + "-missing", null, null, null));
        assertTrue(posts.adminPageCount(null, null, null, null) >= 3,
                "条件全为 null 时必须退化成统计全部");

        List<BlogPostEntity> page = posts.adminPage(keyword, null, null, null, 0, 10);
        assertEquals(2, page.size());
        assertEquals(second.getId(), page.get(0).getId(), "后台列表按 updated_at DESC, id DESC");
        assertEquals(first.getId(), page.get(1).getId());
        assertEquals(1, posts.adminPage(keyword, null, null, null, 0, 1).size(), "LIMIT 没生效");
        assertEquals(first.getId(), posts.adminPage(keyword, null, null, null, 1, 1).get(0).getId(),
                "OFFSET 没生效");
        assertEquals(1, posts.adminPage(keyword, "PUBLISHED", null, null, 0, 10).size());
        assertEquals(1, posts.adminPage(keyword, null, tag.getId(), null, 0, 10).size());
    }

    @Test
    void metaCoverAndBodyUpdatesOnlyTouchTheirOwnColumns() {
        String slug = unique("mapperxmlpost");
        String title = unique("mapperxmlposttitle");
        BlogPostEntity post = newPost(slug, title, "PUBLISHED", PROBE_BASE);
        posts.insertPost(post);

        long toucher = probeId();
        assertEquals(1, posts.updatePostMeta(post.getId(), slug + "-renamed", title + "-改名",
                "新摘要", toucher));
        BlogPostEntity renamed = posts.postById(post.getId());
        assertEquals(slug + "-renamed", renamed.getSlug());
        assertEquals(title + "-改名", renamed.getTitle());
        assertEquals("新摘要", renamed.getSummary());
        assertEquals(toucher, renamed.getUpdatedByAccountId().longValue());
        assertEquals("# XML 验证正文", renamed.getBodyMarkdown(), "改元数据不应动正文");
        assertEquals("PUBLISHED", renamed.getStatus(), "改元数据不应动状态");
        assertNotNull(renamed.getPublishedAt(), "改元数据不应动发布时间");
        assertNull(renamed.getCoverMediaAssetId(), "改元数据不应动封面");

        long coverId = probeId();
        assertEquals(1, posts.updatePostCover(post.getId(), coverId, toucher));
        BlogPostEntity withCover = posts.postById(post.getId());
        assertEquals(Long.valueOf(coverId), withCover.getCoverMediaAssetId());
        assertEquals(title + "-改名", withCover.getTitle(), "换封面不应动标题");
        assertEquals("# XML 验证正文", withCover.getBodyMarkdown(), "换封面不应动正文");

        // 取消封面：updatePostCover 是普通 SET，传 null 就是清空（与「不传」区分开）
        assertEquals(1, posts.updatePostCover(post.getId(), null, toucher));
        assertNull(posts.postById(post.getId()).getCoverMediaAssetId(), "封面传 null 没有被清空");
        assertEquals(Long.valueOf(coverId), withCover.getCoverMediaAssetId());

        assertEquals(1, posts.updatePostBody(post.getId(), "# 新正文", toucher));
        BlogPostEntity withBody = posts.postById(post.getId());
        assertEquals("# 新正文", withBody.getBodyMarkdown());
        assertEquals(title + "-改名", withBody.getTitle(), "改正文不应动标题");
        assertEquals(slug + "-renamed", withBody.getSlug(), "改正文不应动地址");
        assertNull(withBody.getCoverMediaAssetId(), "改正文不应把已清空的封面写回来");

        assertEquals(0, posts.updatePostMeta(-1L, "x", "x", "x", toucher));
        assertEquals(0, posts.updatePostCover(-1L, coverId, toucher));
        assertEquals(0, posts.updatePostBody(-1L, "x", toucher));
    }

    @Test
    void publishWithdrawAndDeleteFollowTheirStateGuards() {
        String slug = unique("mapperxmlpost");
        BlogPostEntity post = newPost(slug, unique("mapperxmlposttitle"), "DRAFT", null);
        posts.insertPost(post);

        LocalDateTime firstPublishedAt = PROBE_BASE.plusDays(2);
        assertEquals(1, posts.publishPost(post.getId(), firstPublishedAt, probeId()),
                "DRAFT → PUBLISHED 应当成功");
        BlogPostEntity published = posts.postById(post.getId());
        assertEquals("PUBLISHED", published.getStatus());
        assertEquals(firstPublishedAt, published.getPublishedAt());
        assertNull(published.getWithdrawnAt());
        assertEquals(0, posts.publishPost(post.getId(), PROBE_BASE.plusDays(3), probeId()),
                "已发布的不能被重复发布");
        assertEquals(firstPublishedAt, posts.postById(post.getId()).getPublishedAt(),
                "被拒绝的发布不应改动发布时间");

        LocalDateTime withdrawnAt = PROBE_BASE.plusDays(4);
        assertEquals(1, posts.withdrawPost(post.getId(), withdrawnAt, probeId()),
                "PUBLISHED → WITHDRAWN 应当成功");
        BlogPostEntity withdrawn = posts.postById(post.getId());
        assertEquals("WITHDRAWN", withdrawn.getStatus());
        assertEquals(withdrawnAt, withdrawn.getWithdrawnAt());
        assertEquals(0, posts.withdrawPost(post.getId(), withdrawnAt, probeId()),
                "已撤回的不能再撤回");
        assertNull(posts.publishedPostById(post.getId()), "已撤回文章不应出现在公开详情里");

        // 撤回后恢复：published_at 用 COALESCE 保住首次发布时间
        assertEquals(1, posts.publishPost(post.getId(), PROBE_BASE.plusDays(5), probeId()),
                "WITHDRAWN → PUBLISHED 应当成功");
        BlogPostEntity restored = posts.postById(post.getId());
        assertEquals(firstPublishedAt, restored.getPublishedAt(),
                "恢复已撤回文章不能改写成「今天发布」（COALESCE 丢失）");
        assertNull(restored.getWithdrawnAt(), "重新发布必须清掉 withdrawn_at");
        assertNotNull(posts.publishedPostById(post.getId()));

        assertEquals(0, posts.deletePost(post.getId()), "PUBLISHED 的公开内容不能被直接删除");
        assertNotNull(posts.postById(post.getId()));
        assertEquals(1, posts.withdrawPost(post.getId(), withdrawnAt, probeId()));
        assertEquals(1, posts.deletePost(post.getId()), "WITHDRAWN 的文章应当可以物理删除");
        assertNull(posts.postById(post.getId()));
        assertEquals(0, posts.deletePost(post.getId()));

        BlogPostEntity fresh = newPost(unique("mapperxmlpost"), unique("mapperxmlposttitle"), "DRAFT", null);
        posts.insertPost(fresh);
        assertEquals(1, posts.deletePost(fresh.getId()), "DRAFT 的文章应当可以物理删除");
    }

    @Test
    void publishedPagesHonourTagTopicAndTimeFilters() {
        String tagSlug = unique("mapperxmltag");
        String topicSlug = unique("mapperxmltopic");
        BlogTagEntity tag = newTag(tagSlug, unique("mapperxmltagname"), "ENABLED");
        tags.insertTag(tag);
        BlogTopicEntity topic = newTopic(topicSlug, unique("mapperxmltopicname"), "ENABLED");
        topics.insertTopic(topic);

        BlogPostEntity first = newPost(unique("mapperxmlpost"), unique("mapperxmlposttitle"),
                "PUBLISHED", PROBE_BASE);
        posts.insertPost(first);
        BlogPostEntity second = newPost(unique("mapperxmlpost"), unique("mapperxmlposttitle"),
                "PUBLISHED", PROBE_BASE.plusDays(1));
        posts.insertPost(second);
        BlogPostEntity third = newPost(unique("mapperxmlpost"), unique("mapperxmlposttitle"),
                "PUBLISHED", PROBE_BASE.plusDays(2));
        posts.insertPost(third);
        BlogPostEntity draft = newPost(unique("mapperxmlpost"), unique("mapperxmlposttitle"), "DRAFT", null);
        posts.insertPost(draft);

        assertEquals(1, tags.insertPostTag(first.getId(), tag.getId()));
        assertEquals(1, tags.insertPostTag(second.getId(), tag.getId()));
        assertEquals(1, tags.insertPostTag(draft.getId(), tag.getId()));
        // 专题成员按人工策展顺序：third 排在 second 前面，与发布时间相反
        assertEquals(1, topics.insertTopicPost(topic.getId(), second.getId(), 20));
        assertEquals(1, topics.insertTopicPost(topic.getId(), third.getId(), 10));

        LocalDateTime from = PROBE_BASE;
        LocalDateTime to = PROBE_BASE.plusDays(5);
        assertEquals(3, posts.publishedPageCount(null, null, from, to),
                "时间窗内应当只有探针文章（草稿不算）");
        assertEquals(0, posts.publishedPageCount(null, null, to, to.plusDays(1)),
                "publishedTo 是开区间，窗外的文章不该命中");
        assertEquals(2, posts.publishedPageCount(tagSlug, null, from, to),
                "tagSlug 过滤没生效（标签绑了两篇已发布文章，草稿那条不算）");
        assertEquals(0, posts.publishedPageCount(unique("mapperxmltag-missing"), null, from, to));
        assertEquals(2, posts.publishedPageCount(null, topicSlug, from, to), "topicSlug 过滤没生效");
        assertEquals(0, posts.publishedPageCount(null, unique("mapperxmltopic-missing"), from, to));

        List<BlogPostEntity> page = posts.publishedPage(null, null, from, to, 0, 10);
        assertEquals(3, page.size());
        assertEquals(third.getId(), page.get(0).getId(), "公开列表按 published_at DESC, id DESC");
        assertEquals(first.getId(), page.get(2).getId());
        assertEquals(1, posts.publishedPage(null, null, from, to, 0, 1).size(), "LIMIT 没生效");
        assertEquals(second.getId(), posts.publishedPage(null, null, from, to, 1, 1).get(0).getId(),
                "OFFSET 没生效");
        assertFalse(page.stream().anyMatch(item -> item.getId().equals(draft.getId())),
                "草稿不应出现在公开列表里");
        List<BlogPostEntity> tagged = posts.publishedPage(tagSlug, null, from, to, 0, 10);
        assertEquals(2, tagged.size());
        assertEquals(second.getId(), tagged.get(0).getId(), "公开列表按 published_at DESC");

        assertEquals(2, posts.publishedTopicPageCount(topicSlug));
        List<BlogPostEntity> curated = posts.publishedTopicPage(topicSlug, 0, 10);
        assertEquals(2, curated.size());
        assertEquals(third.getId(), curated.get(0).getId(),
                "专题页必须按 sr_blog_topic_post.sort_order 排（策展顺序），不是时间倒序");
        assertEquals(second.getId(), curated.get(1).getId());
        assertEquals(1, posts.publishedTopicPage(topicSlug, 0, 1).size(), "LIMIT 没生效");
        assertEquals(second.getId(), posts.publishedTopicPage(topicSlug, 1, 1).get(0).getId(),
                "OFFSET 没生效");
        assertEquals(0, posts.publishedTopicPageCount(unique("mapperxmltopic-missing")));

        assertEquals(1, posts.publishedByTopicSlug(topicSlug, 1).size(), "LIMIT 没生效");
        assertEquals(third.getId(), posts.publishedByTopicSlug(topicSlug, 1).get(0).getId());
        assertEquals(1, posts.publishedByTopicSlug(topicSlug, 10).stream()
                .filter(item -> item.getId().equals(second.getId())).count());
    }

    @Test
    void neighbourCursorArchiveAndLatestQueriesWork() {
        BlogPostEntity first = newPost(unique("mapperxmlpost"), unique("mapperxmlposttitle"),
                "PUBLISHED", PROBE_BASE);
        posts.insertPost(first);
        BlogPostEntity second = newPost(unique("mapperxmlpost"), unique("mapperxmlposttitle"),
                "PUBLISHED", PROBE_BASE.plusDays(1));
        posts.insertPost(second);
        BlogPostEntity third = newPost(unique("mapperxmlpost"), unique("mapperxmlposttitle"),
                "PUBLISHED", PROBE_BASE.plusDays(2));
        posts.insertPost(third);

        BlogPostNeighborVO previous = posts.publishedPrevious(second.getPublishedAt(), second.getId());
        assertNotNull(previous, "中间那篇应当有上一篇");
        assertEquals(first.getSlug(), previous.getSlug(), "别名 slug/title 丢失或上一篇取错");
        assertEquals(first.getTitle(), previous.getTitle());
        BlogPostNeighborVO next = posts.publishedNext(second.getPublishedAt(), second.getId());
        assertNotNull(next, "中间那篇应当有下一篇");
        assertEquals(third.getSlug(), next.getSlug());
        assertEquals(third.getTitle(), next.getTitle());
        // 最新一篇没有「下一篇」（真实库里没有 2099 年的文章）
        assertNull(posts.publishedNext(third.getPublishedAt(), third.getId()));
        // 第一篇的「上一篇」是最新的一篇真实文章（开发库里有已发布文章），只断言它不是探针文章
        BlogPostNeighborVO older = posts.publishedPrevious(first.getPublishedAt(), first.getId());
        assertNotNull(older, "第一篇探针文章之前应当还有真实文章");
        assertFalse(older.getSlug().equals(first.getSlug()));
        assertFalse(older.getSlug().equals(third.getSlug()));

        List<BlogPostEntity> latest = posts.latestPublished(3);
        assertEquals(3, latest.size(), "latestPublished 应当取到最新的 3 篇（探针文章是 2099 年，最靠前）");
        assertEquals(third.getId(), latest.get(0).getId());
        assertEquals(first.getId(), latest.get(2).getId());
        assertEquals(1, posts.latestPublished(1).size(), "LIMIT 没生效");

        List<BlogPostEntity> cursor = posts.publishedAfterCursor(second.getId(), 10);
        assertEquals(1, cursor.size(), "游标之后的已发布文章应当只有第三篇");
        assertEquals(third.getId(), cursor.get(0).getId());
        assertTrue(posts.publishedAfterCursor(third.getId(), 10).isEmpty());
        assertEquals(3, posts.publishedAfterCursor(null, 3).size(),
                "cursorId 为 null 时不应加 id > ? 条件，应当直接按 id 升序取 LIMIT 条");
        assertEquals(3, posts.publishedAfterCursor(0L, 3).size(),
                "id > 0 与不加条件等价，用来证明游标条件只在非 null 时拼接");

        List<BlogArchiveMonthVO> months = posts.archiveMonths();
        BlogArchiveMonthVO probeMonth = months.stream()
                .filter(item -> Integer.valueOf(2099).equals(item.getYear())
                        && Integer.valueOf(1).equals(item.getMonth()))
                .findFirst().orElse(null);
        assertNotNull(probeMonth, "归档月份里找不到探针文章的 2099-01 桶");
        assertEquals(3L, probeMonth.getPostCount().longValue(), "别名 postCount 丢失或计数不对");

        List<BlogArchiveDayVO> days = posts.archiveDays(PROBE_BASE, PROBE_BASE.plusDays(5));
        assertEquals(3, days.size());
        assertEquals(1, days.get(0).getDay().intValue(), "归档按日升序");
        assertEquals(1L, days.get(0).getPostCount().longValue(), "别名 postCount 丢失");
        assertEquals(3, days.get(2).getDay().intValue());
        assertTrue(posts.archiveDays(PROBE_BASE.plusDays(10), PROBE_BASE.plusDays(20)).isEmpty(),
                "时间窗外不应有归档桶");
    }

    @Test
    void tagMapperRoundTripsCrudUniquenessAndAdminCounts() {
        String slug = unique("mapperxmltag");
        String name = unique("mapperxmltagname");
        BlogTagEntity tag = newTag(slug, name, "ENABLED");
        tags.insertTag(tag);
        assertNotNull(tag.getId(), "insertTag 没有回填自增主键");

        BlogTagEntity loaded = tags.tagById(tag.getId());
        assertNotNull(loaded);
        assertEquals(slug, loaded.getSlug());
        assertEquals(name, loaded.getName());
        assertEquals("XML 验证标签说明", loaded.getDescription());
        assertEquals("ENABLED", loaded.getStatus());
        assertNotNull(loaded.getCreatedAt(), "created_at 由列默认值填充，应能读回");
        assertNotNull(tags.tagByIdForUpdate(tag.getId()));
        assertNull(tags.tagById(-1L));

        assertEquals(1, tags.countBySlug(slug, null));
        assertEquals(0, tags.countBySlug(slug, tag.getId()));
        assertEquals(1, tags.countByName(name, null));
        assertEquals(0, tags.countByName(name, tag.getId()));
        assertEquals(0, tags.countBySlug(unique("mapperxmltag-missing"), null));

        assertEquals(1, tags.updateTag(tag.getId(), slug + "-renamed", name + "-改名", "改后的说明"));
        BlogTagEntity renamed = tags.tagById(tag.getId());
        assertEquals(slug + "-renamed", renamed.getSlug());
        assertEquals(name + "-改名", renamed.getName());
        assertEquals("改后的说明", renamed.getDescription());
        assertEquals("ENABLED", renamed.getStatus(), "改文案不应动 status");

        // description 是普通 SET：传 null 就是清空
        assertEquals(1, tags.updateTag(tag.getId(), slug + "-renamed", name + "-改名", null));
        assertNull(tags.tagById(tag.getId()).getDescription(), "description 传 null 没有被清空");

        assertEquals(1, tags.updateStatus(tag.getId(), "DISABLED"));
        assertEquals("DISABLED", tags.tagById(tag.getId()).getStatus());
        assertEquals(name + "-改名", tags.tagById(tag.getId()).getName(), "改状态不应动名字");
        assertEquals(1, tags.updateStatus(tag.getId(), "ENABLED"));
        assertEquals(0, tags.updateStatus(-1L, "DISABLED"));

        assertEquals(1, tags.adminPageCount(name + "-改名", null), "keyword 过滤（name）没生效");
        assertEquals(1, tags.adminPageCount(name + "-改名", "ENABLED"), "status 过滤没生效");
        assertEquals(0, tags.adminPageCount(name + "-改名", "DISABLED"));
        assertEquals(0, tags.adminPageCount(name + "-missing", null));
        assertEquals(1, tags.adminPage(name + "-改名", null, 0, 10).size());
        BlogTagVO vo = tags.adminPage(name + "-改名", null, 0, 10).get(0);
        assertEquals(tag.getId(), vo.getId());
        assertEquals(slug + "-renamed", vo.getSlug());
        assertEquals(name + "-改名", vo.getName());
        assertEquals("ENABLED", vo.getStatus());
        assertNotNull(vo.getPostCount(), "别名 postCount 丢失（XML 用的是 COUNT(pt.id) AS post_count）");
        assertEquals(0L, vo.getPostCount().longValue(), "还没有绑定文章时 postCount 必须是 0");
        assertTrue(tags.adminPageCount(null, null) >= 1, "条件全为 null 时必须退化成统计全部标签");
        assertEquals(1, tags.adminPage(name + "-改名", null, 0, 1).size(), "LIMIT 没生效");
        assertTrue(tags.adminPage(name + "-改名", null, 1, 10).isEmpty(), "OFFSET 没生效");
    }

    @Test
    void tagBindingsDriveAdminPublicAndBulkQueries() {
        String slug = unique("mapperxmltag");
        String name = unique("mapperxmltagname");
        BlogTagEntity tag = newTag(slug, name, "ENABLED");
        tags.insertTag(tag);
        BlogTagEntity disabled = newTag(unique("mapperxmltag"), unique("mapperxmltagname"), "DISABLED");
        tags.insertTag(disabled);
        BlogTagEntity draftOnly = newTag(unique("mapperxmltag"), unique("mapperxmltagname"), "ENABLED");
        tags.insertTag(draftOnly);

        BlogPostEntity published = newPost(unique("mapperxmlpost"), unique("mapperxmlposttitle"),
                "PUBLISHED", PROBE_BASE);
        posts.insertPost(published);
        BlogPostEntity another = newPost(unique("mapperxmlpost"), unique("mapperxmlposttitle"),
                "PUBLISHED", PROBE_BASE.plusDays(1));
        posts.insertPost(another);
        BlogPostEntity draft = newPost(unique("mapperxmlpost"), unique("mapperxmlposttitle"), "DRAFT", null);
        posts.insertPost(draft);

        assertEquals(1, tags.insertPostTag(published.getId(), tag.getId()));
        assertEquals(1, tags.insertPostTag(another.getId(), tag.getId()));
        assertEquals(1, tags.insertPostTag(draft.getId(), tag.getId()));
        assertEquals(1, tags.insertPostTag(draft.getId(), disabled.getId()));
        assertEquals(1, tags.insertPostTag(draft.getId(), draftOnly.getId()));
        assertEquals(1L, tags.countPostTag(published.getId(), tag.getId()));
        assertEquals(0L, tags.countPostTag(published.getId(), disabled.getId()));
        assertEquals(1L, tags.countDisabledTagsByPostId(draft.getId()),
                "正文绑定了一个被停用的标签，发布前校验应当能查出来");
        assertEquals(0L, tags.countDisabledTagsByPostId(published.getId()));

        // 后台计数含草稿，前台计数只算已发布
        BlogTagVO admin = tags.adminPage(name, null, 0, 10).get(0);
        assertEquals(3L, admin.getPostCount().longValue(), "后台 postCount 应当含草稿与已撤回");
        BlogTagVO publicVo = tags.publishedTags().stream()
                .filter(item -> tag.getId().equals(item.getId())).findFirst().orElse(null);
        assertNotNull(publicVo, "至少有一篇已发布文章的标签应当出现在前台筛选项里");
        assertEquals(2L, publicVo.getPostCount().longValue(), "前台 postCount 只应统计已发布文章");
        assertNull(tags.publishedTags().stream()
                        .filter(item -> disabled.getId().equals(item.getId())).findFirst().orElse(null),
                "被停用的标签不应出现在前台筛选项里");
        assertNull(tags.publishedTags().stream()
                        .filter(item -> draftOnly.getId().equals(item.getId())).findFirst().orElse(null),
                "只绑了草稿的标签不应出现在前台筛选项里（HAVING COUNT(p.id) > 0 丢失）");

        List<BlogTagVO> byPost = tags.tagsByPostId(draft.getId());
        assertEquals(3, byPost.size(), "后台要能看到停用标签才能重新启用它");
        assertTrue(tags.tagsByPostId(probeId()).isEmpty());

        List<BlogPostTagRow> rows = tags.tagsByPostIds(List.of(published.getId(), another.getId()));
        assertEquals(2, rows.size(), "批量取标签没有按 post_id IN (...) 命中两行");
        for (BlogPostTagRow row : rows) {
            assertNotNull(row.getPostId(), "别名 postId 丢失（XML 用的是 pt.post_id AS post_id）");
            assertNotNull(row.getTagId(), "别名 tagId 丢失");
            assertNotNull(row.getSlug(), "别名 slug 丢失");
            assertNotNull(row.getName(), "别名 name 丢失");
        }
        assertTrue(rows.stream().anyMatch(row -> row.getPostId().equals(another.getId())
                && row.getTagId().equals(tag.getId())));

        List<BlogTagEntity> byIds = tags.tagsByIds(List.of(tag.getId(), disabled.getId()));
        assertEquals(2, byIds.size());
        assertTrue(byIds.stream().anyMatch(item -> "DISABLED".equals(item.getStatus())));

        assertEquals(1, tags.deletePostTag(draft.getId(), disabled.getId()));
        assertEquals(0L, tags.countDisabledTagsByPostId(draft.getId()));
        assertEquals(0, tags.deletePostTag(draft.getId(), disabled.getId()));
        assertEquals(2, tags.deletePostTagsByPostId(draft.getId()),
                "一次删掉草稿剩下的两条标签绑定");
        assertEquals(0, tags.deletePostTagsByPostId(draft.getId()));
        assertTrue(tags.tagsByPostId(draft.getId()).isEmpty());
    }

    @Test
    void topicMapperRoundTripsCrudCuratedOrderAndMemberQueries() {
        String slug = unique("mapperxmltopic");
        BlogTopicEntity topic = newTopic(slug, unique("mapperxmltopicname"), "ENABLED");
        topics.insertTopic(topic);
        assertNotNull(topic.getId(), "insertTopic 没有回填自增主键");

        BlogTopicEntity loaded = topics.topicById(topic.getId());
        assertNotNull(loaded);
        assertEquals(slug, loaded.getSlug());
        assertEquals("ENABLED", loaded.getStatus());
        assertEquals("XML 验证专题说明", loaded.getDescription());
        assertNotNull(loaded.getCreatedAt(), "created_at 由列默认值填充，应能读回");
        assertNotNull(topics.topicByIdForUpdate(topic.getId()));
        assertNull(topics.topicById(-1L));

        assertEquals(1, topics.countBySlug(slug, null));
        assertEquals(0, topics.countBySlug(slug, topic.getId()));
        assertEquals(0, topics.countBySlug(unique("mapperxmltopic-missing"), null));

        assertEquals(1, topics.updateTopic(topic.getId(), slug + "-renamed", "改名后的专题", null));
        BlogTopicEntity renamed = topics.topicById(topic.getId());
        assertEquals(slug + "-renamed", renamed.getSlug());
        assertEquals("改名后的专题", renamed.getName());
        assertNull(renamed.getDescription(), "description 传 null 没有被清空");
        assertEquals("ENABLED", renamed.getStatus(), "改文案不应动 status");
        assertEquals(1, topics.updateStatus(topic.getId(), "DISABLED"));
        assertEquals("DISABLED", topics.topicById(topic.getId()).getStatus());
        assertEquals(1, topics.updateStatus(topic.getId(), "ENABLED"));
        assertEquals(0, topics.updateStatus(-1L, "DISABLED"));

        BlogPostEntity first = newPost(unique("mapperxmlpost"), unique("mapperxmlposttitle"),
                "PUBLISHED", PROBE_BASE);
        posts.insertPost(first);
        BlogPostEntity second = newPost(unique("mapperxmlpost"), unique("mapperxmlposttitle"),
                "DRAFT", null);
        posts.insertPost(second);
        BlogPostEntity third = newPost(unique("mapperxmlpost"), unique("mapperxmlposttitle"),
                "PUBLISHED", PROBE_BASE.plusDays(1));
        posts.insertPost(third);

        assertNull(topics.maxSortOrder(topic.getId()), "还没有成员时 maxSortOrder 应当是 null");
        assertEquals(1, topics.insertTopicPost(topic.getId(), first.getId(), 10));
        assertEquals(1, topics.insertTopicPost(topic.getId(), second.getId(), 20));
        assertEquals(1, topics.insertTopicPost(topic.getId(), third.getId(), 30));
        assertEquals(3L, topics.countTopicPosts(topic.getId()));
        assertEquals(1L, topics.countTopicPost(topic.getId(), second.getId()));
        assertEquals(0L, topics.countTopicPost(topic.getId(), probeId()));
        assertEquals(30, topics.maxSortOrder(topic.getId()).intValue());

        List<BlogTopicMemberRow> members = topics.topicMembers(topic.getId());
        assertEquals(3, members.size(), "专题成员应当按 sort_order 升序返回");
        assertEquals(first.getId(), members.get(0).getPostId(), "别名 postId 丢失或顺序不对");
        assertEquals(first.getSlug(), members.get(0).getSlug(), "别名 slug 丢失");
        assertEquals(first.getTitle(), members.get(0).getTitle(), "别名 title 丢失");
        assertEquals("PUBLISHED", members.get(0).getStatus(), "别名 status 丢失");
        assertEquals(10, members.get(0).getSortOrder().intValue(), "别名 sortOrder 丢失");
        assertNotNull(members.get(0).getPublishedAt(), "别名 publishedAt 丢失");
        assertNotNull(members.get(0).getUpdatedAt(), "别名 updatedAt 丢失");
        assertEquals("DRAFT", members.get(1).getStatus());
        assertNull(members.get(1).getPublishedAt(), "草稿成员没有发布时间");

        // 人工重排：把第三条挪到最前
        assertEquals(2, topics.updateTopicOrder(topic.getId(), List.of(
                BlogTopicOrderItem.builder().postId(third.getId()).sortOrder(1).build(),
                BlogTopicOrderItem.builder().postId(first.getId()).sortOrder(2).build())));
        List<BlogTopicMemberRow> reordered = topics.topicMembers(topic.getId());
        assertEquals(third.getId(), reordered.get(0).getPostId(), "updateTopicOrder 没有写进新的序号");
        assertEquals(1, reordered.get(0).getSortOrder().intValue());
        assertEquals(second.getId(), reordered.get(2).getPostId(),
                "不在 items 里的成员序号不应被改动");

        BlogPostTopicRow row = topics.topicsByPostIds(List.of(first.getId(), third.getId())).get(0);
        assertNotNull(row.getPostId(), "别名 postId 丢失（XML 用的是 tp.post_id AS post_id）");
        assertNotNull(row.getTopicId(), "别名 topicId 丢失");
        assertNotNull(row.getSlug(), "别名 slug 丢失");
        assertNotNull(row.getName(), "别名 name 丢失");
        assertNotNull(row.getSortOrder(), "别名 sortOrder 丢失");

        assertEquals(1, topics.deleteTopicPost(topic.getId(), second.getId()));
        assertEquals(0, topics.deleteTopicPost(topic.getId(), second.getId()));
        assertEquals(2, topics.countTopicPosts(topic.getId()));
        assertEquals(1, topics.deleteTopicPostsByPostId(first.getId()),
                "按文章清理专题关系应当只删掉那一条");
        assertEquals(0, topics.deleteTopicPostsByPostId(first.getId()));
    }

    @Test
    void topicVisibilityDrivesEveryPublicQuery() {
        String enabledSlug = unique("mapperxmltopic");
        BlogTopicEntity enabled = newTopic(enabledSlug, unique("mapperxmltopicname"), "ENABLED");
        topics.insertTopic(enabled);
        BlogTopicEntity disabled = newTopic(unique("mapperxmltopic"), unique("mapperxmltopicname"), "DISABLED");
        topics.insertTopic(disabled);
        BlogTopicEntity draftOnly = newTopic(unique("mapperxmltopic"), unique("mapperxmltopicname"), "ENABLED");
        topics.insertTopic(draftOnly);

        BlogPostEntity published = newPost(unique("mapperxmlpost"), unique("mapperxmlposttitle"),
                "PUBLISHED", PROBE_BASE);
        posts.insertPost(published);
        BlogPostEntity draft = newPost(unique("mapperxmlpost"), unique("mapperxmlposttitle"), "DRAFT", null);
        posts.insertPost(draft);
        BlogPostEntity noTopic = newPost(unique("mapperxmlpost"), unique("mapperxmlposttitle"),
                "PUBLISHED", PROBE_BASE.plusDays(1));
        posts.insertPost(noTopic);

        assertEquals(1, topics.insertTopicPost(enabled.getId(), published.getId(), 1));
        assertEquals(1, topics.insertTopicPost(enabled.getId(), draft.getId(), 2));
        assertEquals(1, topics.insertTopicPost(disabled.getId(), published.getId(), 1));
        assertEquals(1, topics.insertTopicPost(draftOnly.getId(), draft.getId(), 1));
        assertEquals(1L, topics.countTopicPosts(draftOnly.getId()));
        BlogTopicVO admin = topics.adminPage(enabledSlug, null, 0, 10).get(0);
        assertEquals(2L, admin.getMemberCount().longValue(),
                "后台 memberCount 应当含未发布成员（别名 memberCount 丢失）");
        assertEquals(1L, topics.adminPageCount(enabledSlug, "ENABLED"));

        BlogTopicVO publicTopic = topics.publishedTopics().stream()
                .filter(item -> enabled.getId().equals(item.getId())).findFirst().orElse(null);
        assertNotNull(publicTopic, "ENABLED 且有已发布成员的专题应当出现在前台入口");
        assertEquals(1L, publicTopic.getMemberCount().longValue(), "前台 memberCount 只算已发布成员");
        assertNull(topics.publishedTopics().stream()
                        .filter(item -> disabled.getId().equals(item.getId())).findFirst().orElse(null),
                "停用专题不应出现在前台入口");
        assertNull(topics.publishedTopics().stream()
                        .filter(item -> draftOnly.getId().equals(item.getId())).findFirst().orElse(null),
                "只有草稿成员的专题不应出现在前台入口（HAVING COUNT(p.id) > 0 丢失）");

        BlogTopicVO detail = topics.publishedTopicBySlug(enabledSlug);
        assertNotNull(detail, "前台专题页应当能按 slug 打开");
        assertEquals(enabled.getId(), detail.getId());
        assertEquals(1L, detail.getMemberCount().longValue(), "别名 memberCount 丢失");
        assertNull(topics.publishedTopicBySlug(unique("mapperxmltopic-missing")));

        List<BlogTopicVO> publicTopics = topics.publicTopicsByPostId(published.getId());
        assertEquals(1, publicTopics.size(), "停用专题不应出现在公开页面的标记里");
        assertEquals(enabled.getId(), publicTopics.get(0).getId());
        assertEquals(enabledSlug, publicTopics.get(0).getSlug());
        assertEquals(2, topics.topicsByPostId(published.getId()).size(),
                "后台要能看到停用专题才能重新启用它");
        assertTrue(topics.publicTopicsByPostId(noTopic.getId()).isEmpty(),
                "没有绑定任何专题的文章没有公开专题标记");
        assertEquals(2, topics.publicTopicsByPostId(draft.getId()).size(),
                "草稿绑了两个启用中的专题，公开标记里都应当在（停用的那个不在）");

        List<BlogPostTopicRow> publicRows = topics.publicTopicsByPostIds(List.of(published.getId()));
        assertEquals(1, publicRows.size(), "批量公开标记里不应带上停用专题");
        assertNotNull(publicRows.get(0).getSortOrder(), "别名 sortOrder 丢失");
        List<BlogPostTopicRow> adminRows = topics.topicsByPostIds(List.of(published.getId()));
        assertEquals(2, adminRows.size(), "批量后台标记里应当带上停用专题");
    }

    @Test
    void rollbackLeavesNoBlogProbeRowsBehind() {
        BlogTagEntity tag = newTag(unique("mapperxmlrollback"), unique("mapperxmlrollbackname"), "ENABLED");
        tags.insertTag(tag);
        BlogTopicEntity topic = newTopic(unique("mapperxmlrollback"), unique("mapperxmlrollbackname"), "ENABLED");
        topics.insertTopic(topic);
        BlogPostEntity post = newPost(unique("mapperxmlrollback"), unique("mapperxmlrollbacktitle"),
                "DRAFT", null);
        posts.insertPost(post);
        tags.insertPostTag(post.getId(), tag.getId());
        topics.insertTopicPost(topic.getId(), post.getId(), 1);
        Long postId = post.getId();
        Long tagId = tag.getId();
        Long topicId = topic.getId();
        assertNotNull(postId);

        session.rollback();
        try (SqlSession fresh = openSession()) {
            BlogPostMapper freshPosts = fresh.getMapper(BlogPostMapper.class);
            assertNull(freshPosts.postById(postId), "回滚后不应在开发库里留下测试文章");
            assertNull(fresh.getMapper(BlogTagMapper.class).tagById(tagId),
                    "回滚后不应在开发库里留下测试标签");
            assertNull(fresh.getMapper(BlogTopicMapper.class).topicById(topicId),
                    "回滚后不应在开发库里留下测试专题");
            assertTrue(fresh.getMapper(BlogTagMapper.class).tagsByPostId(postId).isEmpty());
            assertTrue(fresh.getMapper(BlogTopicMapper.class).topicsByPostId(postId).isEmpty());
        }
    }

    private BlogPostEntity newPost(String slug, String title, String status, LocalDateTime publishedAt) {
        BlogPostEntity post = new BlogPostEntity();
        post.setSlug(slug);
        post.setTitle(title);
        post.setSummary("XML 验证摘要");
        post.setBodyMarkdown("# XML 验证正文");
        post.setCoverMediaAssetId(null);
        post.setStatus(status);
        post.setPublishedAt(publishedAt);
        post.setWithdrawnAt(null);
        post.setCreatedByAccountId(probeId());
        post.setUpdatedByAccountId(post.getCreatedByAccountId());
        return post;
    }

    private BlogTagEntity newTag(String slug, String name, String status) {
        BlogTagEntity tag = new BlogTagEntity();
        tag.setSlug(slug);
        tag.setName(name);
        tag.setDescription("XML 验证标签说明");
        tag.setStatus(status);
        return tag;
    }

    private BlogTopicEntity newTopic(String slug, String name, String status) {
        BlogTopicEntity topic = new BlogTopicEntity();
        topic.setSlug(slug);
        topic.setName(name);
        topic.setDescription("XML 验证专题说明");
        topic.setStatus(status);
        return topic;
    }

    /* 逻辑外键，库里没有物理 FOREIGN KEY：用一个大号段避免和真实账户撞号 */
    private long probeId() {
        return 900_000_000_000L + PROBE.incrementAndGet();
    }

    private String unique(String prefix) {
        return prefix + "-" + PROBE.incrementAndGet() + "-" + System.nanoTime();
    }
}
