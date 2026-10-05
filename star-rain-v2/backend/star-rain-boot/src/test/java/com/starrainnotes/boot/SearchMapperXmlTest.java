package com.starrainnotes.boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.starrainnotes.search.api.dto.SearchableDocument;
import com.starrainnotes.search.mapper.SearchDocumentMapper;
import com.starrainnotes.search.vo.SearchHitVO;
import com.starrainnotes.search.vo.SearchSuggestionVO;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/*
 * search 模块 SearchDocumentMapper 的真实数据库读写验证。
 *
 * upsert 走 `INSERT ... ON DUPLICATE KEY UPDATE`；search / suggestions 返回 VO，
 * 列别名写错不会有任何类型检查报错，只会让 `hit.getRoutePath()` 静默变成 null。
 * 所以这里逐字段断言，尤其：
 *   contentId 由 `CAST(content_id AS CHAR)` 得到，声明成 String，最容易在迁移中丢别名；
 *   score 是 MATCH + LOCATE 的加权表达式，别名丢了自己算不出来。
 *
 * 关于「精确条数」：sr_search_document 的全文索引建的是
 *   FULLTEXT KEY ft_sr_search_document (title, summary, searchable_text) WITH PARSER ngram
 * ngram 解析器会把查询词切成 bigram，而 `MATCH ... AGAINST(? IN NATURAL LANGUAGE MODE)`
 * 对 ngram 索引是「命中任意一个 bigram 即得分 > 0」。所以一个长的字母数字查询词会命中大量
 * 无关文档（本机实测：一个随机词命中 948/2234 行），count(query, null) 没法断言精确值 ——
 * 这是 ngram 解析器的既定语义，不是 XML 缺陷。
 * 因此本类用**唯一的 content_type** 隔离探针文档：typeFilter 一进来就只剩探针那一行，
 * 精确条数、别名、LIMIT/OFFSET 全都能断言；不带类型过滤时则断言「命中数恰好 +1」。
 * suggestions 只有 LOCATE 分支（精确子串），不受 ngram 影响，可以直接断言精确条数。
 *
 * 三条 remove* 都是把 status 改成 REMOVED 的软删除，靠 WHERE 条件生效
 * （removeRoutePrefix 还硬编码了 content_type = 'CHAPTER'）。
 *
 * 全程在一个不 commit 的 SqlSession 里跑，@AfterEach 回滚；最后一个用例用新连接复核。
 */
class SearchMapperXmlTest extends MapperXmlIntegrationSupport {

    private static final AtomicLong PROBE = new AtomicLong(System.nanoTime() % 1_000_000L);

    private SqlSession session;
    private SearchDocumentMapper documents;

    @BeforeEach
    void open() {
        session = openSession();
        documents = session.getMapper(SearchDocumentMapper.class);
    }

    @AfterEach
    void rollback() {
        if (session != null) {
            session.rollback();
            session.close();
        }
    }

    @Test
    void searchDocumentMapperIsReachableThroughTheSession() {
        assertNotNull(documents);
    }

    @Test
    void upsertIndexesANewDocumentAndSearchExposesEveryAlias() {
        String token = token();
        String type = probeType();
        String documentKey = "mapper-xml-test:" + token;
        long contentId = probeId();
        LocalDateTime publishedAt = now().minusDays(1);

        assertEquals(0, documents.count(token, List.of(type)),
                "隔离用的 content_type 在插入前不应当命中任何真实文档");
        long matchesBefore = documents.count(token, null);

        SearchableDocument document = newDocument(documentKey, type, contentId, token,
                "/mapper-xml-test/" + token);
        document.setPublishedAt(publishedAt);
        assertEquals(1, documents.upsert(document), "首次 upsert 应当插入一行");

        assertTrue(documents.activeKeys(type).contains(documentKey),
                "activeKeys 没有带回刚索引的 document_key");
        assertTrue(documents.activeCount() >= 1);

        assertEquals(1, documents.count(token, List.of(type)),
                "全文检索没有命中刚索引的文档（typeFilter 或 MATCH/LOCATE 条件没生效）");
        assertEquals(matchesBefore + 1, documents.count(token, null),
                "不带类型过滤时，多索引一篇文档应当让命中数恰好 +1");
        assertEquals(0, documents.count(token, List.of(probeType())),
                "别的类型不应当命中探针文档");
        assertEquals(1, documents.count(token, List.of(type, probeType())),
                "types 列表里包含命中类型时不该被过滤掉");

        List<SearchHitVO> hits = documents.search(token, List.of(type), 0, 10);
        assertEquals(1, hits.size(), "search 没有命中刚索引的文档");
        SearchHitVO hit = hits.get(0);
        assertEquals(type, hit.getContentType(), "别名 contentType 丢失");
        assertEquals(String.valueOf(contentId), hit.getContentId(),
                "别名 contentId 丢失或没有按字符串映射（XML 用的是 CAST(content_id AS CHAR)）");
        assertEquals(document.getTitle(), hit.getTitle(), "别名 title 丢失");
        assertEquals(document.getSummary(), hit.getSummary(), "别名 summary 丢失");
        assertEquals(document.getRoutePath(), hit.getRoutePath(), "别名 routePath 丢失");
        assertEquals(publishedAt, hit.getPublishedAt(), "别名 publishedAt 丢失");
        assertNotNull(hit.getScore(), "别名 score 丢失（MATCH + LOCATE 的加权表达式）");
        assertTrue(hit.getScore() > 0, "score 应当为正：" + hit.getScore());

        assertEquals(1, documents.search(token, List.of(type), 0, 1).size(), "LIMIT 没生效");
        assertEquals(1, documents.search(token, List.of(type), 0, 10).size());
        assertTrue(documents.search(token, List.of(type), 1, 10).isEmpty(), "OFFSET 没生效");
        assertTrue(documents.search(token, List.of(probeType()), 0, 10).isEmpty(),
                "types 过滤在 search 里没生效");
        assertTrue(documents.search(unique("mapperxmlsearch-missing"), List.of(type), 0, 10).isEmpty());

        // suggestions 只有 LOCATE 分支，不受 ngram 影响，可以断言精确条数
        List<SearchSuggestionVO> suggestions = documents.suggestions(token, 10);
        assertEquals(1, suggestions.size(), "suggestions 没有命中刚索引的文档");
        assertEquals(document.getTitle(), suggestions.get(0).getText(), "别名 text 丢失");
        assertEquals(type, suggestions.get(0).getContentType(), "别名 contentType 丢失");
        assertEquals(document.getRoutePath(), suggestions.get(0).getRoutePath(), "别名 routePath 丢失");
        assertEquals(1, documents.suggestions(token, 1).size(), "LIMIT 没生效");
    }

    @Test
    void reindexingTheSameDocumentKeyUpdatesInsteadOfDuplicating() {
        String token = token();
        String type = probeType();
        String documentKey = "mapper-xml-test:" + token;
        SearchableDocument first = newDocument(documentKey, type, probeId(), token,
                "/mapper-xml-test/" + token);
        assertEquals(1, documents.upsert(first));

        SearchableDocument second = newDocument(documentKey, type, first.getContentId(),
                token + "second", "/mapper-xml-test/" + token + "-moved");
        second.setTitle(token + "-改名后的标题");
        assertEquals(2, documents.upsert(second),
                "ON DUPLICATE KEY UPDATE 真正改动了行时，MySQL 返回 2");

        assertEquals(1, documents.count(token, List.of(type)),
                "同 document_key 的重复索引产生了第二行");
        List<SearchHitVO> hits = documents.search(token, List.of(type), 0, 10);
        assertEquals(1, hits.size());
        assertEquals(second.getTitle(), hits.get(0).getTitle(), "标题没有被更新");
        assertEquals(second.getRoutePath(), hits.get(0).getRoutePath(), "route_path 没有被更新");
        assertEquals(1, documents.suggestions(token, 10).size());
    }

    @Test
    void removeHidesTheDocumentFromEveryActiveQuery() {
        String token = token();
        String type = probeType();
        String documentKey = "mapper-xml-test:" + token;
        documents.upsert(newDocument(documentKey, type, probeId(), token, "/mapper-xml-test/" + token));
        assertEquals(1, documents.count(token, List.of(type)));

        assertEquals(1, documents.remove(documentKey));
        assertFalse(documents.activeKeys(type).contains(documentKey),
                "REMOVED 的文档不应留在 activeKeys 里");
        assertEquals(0, documents.count(token, List.of(type)), "REMOVED 的文档仍然能被搜到");
        assertTrue(documents.search(token, List.of(type), 0, 10).isEmpty(),
                "REMOVED 的文档仍然出现在搜索结果里");
        assertTrue(documents.suggestions(token, 10).isEmpty(), "REMOVED 的文档仍然出现在联想里");
        assertEquals(0, documents.remove("mapper-xml-test:no-such-key-" + token));
    }

    @Test
    void removeByContentHidesEveryDocumentOfThatContent() {
        String token = token();
        String type = probeType();
        long contentId = probeId();
        SearchableDocument document = newDocument("mapper-xml-test:" + token, type, contentId,
                token, "/mapper-xml-test/" + token);
        documents.upsert(document);
        assertEquals(1, documents.count(token, List.of(type)));

        assertEquals(1, documents.removeByContent(type, contentId));
        assertEquals(0, documents.count(token, List.of(type)), "按内容软删除后仍然能被搜到");
        assertFalse(documents.activeKeys(type).contains(document.getDocumentKey()));
        assertEquals(0, documents.removeByContent(type, probeId()), "别的 content_id 不应被误删");
    }

    @Test
    void removeRoutePrefixOnlyTouchesChapterDocuments() {
        String token = token();
        String prefix = "/mapper-xml-test/" + token + "/chapters";
        SearchableDocument chapter = newDocument("mapper-xml-test:chapter:" + token, "CHAPTER", probeId(),
                token + "chapter", prefix + "/1");
        documents.upsert(chapter);
        SearchableDocument tutorial = newDocument("mapper-xml-test:tutorial:" + token, probeType(), probeId(),
                token + "tutorial", prefix + "/1");
        documents.upsert(tutorial);
        // suggestions 是纯 LOCATE 精确匹配，两篇探针文档的标题/摘要都含同一个 token，用它隔离出这两行
        assertEquals(2, documents.suggestions(token, 10).size());

        assertEquals(1, documents.removeRoutePrefix(prefix),
                "removeRoutePrefix 必须只影响 content_type = 'CHAPTER' 的行");
        assertFalse(documents.activeKeys("CHAPTER").contains(chapter.getDocumentKey()));
        assertTrue(documents.activeKeys(tutorial.getContentType()).contains(tutorial.getDocumentKey()),
                "同前缀的非 CHAPTER 文档不能被顺手软删除");
        List<SearchSuggestionVO> left = documents.suggestions(token, 10);
        assertEquals(1, left.size());
        assertEquals(tutorial.getContentType(), left.get(0).getContentType(),
                "留下来的必须是那篇非 CHAPTER 文档");
        assertEquals(0, documents.removeRoutePrefix(prefix + "-no-such-prefix"));
    }

    @Test
    void rollbackLeavesNoSearchProbeRowsBehind() {
        String token = token();
        String type = probeType();
        String documentKey = "mapper-xml-test:" + token;
        documents.upsert(newDocument(documentKey, type, probeId(), token, "/mapper-xml-test/" + token));

        session.rollback();
        try (SqlSession fresh = openSession()) {
            SearchDocumentMapper mapper = fresh.getMapper(SearchDocumentMapper.class);
            assertEquals(0, mapper.count(token, List.of(type)),
                    "回滚后不应在开发库里留下测试检索文档");
            assertFalse(mapper.activeKeys(type).contains(documentKey));
        }
    }

    private SearchableDocument newDocument(String documentKey, String contentType, long contentId,
                                           String token, String routePath) {
        SearchableDocument document = new SearchableDocument();
        document.setContentType(contentType);
        document.setContentId(contentId);
        document.setDocumentKey(documentKey);
        document.setTitle("XML 验证标题 " + token);
        document.setSummary("XML 验证摘要 " + token);
        document.setSearchableText("XML 验证正文 " + token);
        document.setRoutePath(routePath);
        document.setPublishedAt(now());
        document.setSourceUpdatedAt(now());
        return document;
    }

    /* 全文检索的查询词必须是唯一的单个字母数字 token，才能配合 LOCATE 分支稳定隔离探针文档 */
    private String token() {
        return "mapperxmlsearch" + PROBE.incrementAndGet() + "x" + (System.nanoTime() % 100000L);
    }

    /* content_type 是 varchar(30)：用短且唯一的类型把探针文档从 2234 行真实索引里隔离出来 */
    private String probeType() {
        return "MAPPERXMLTEST" + PROBE.incrementAndGet();
    }

    /* 逻辑外键，库里没有物理 FOREIGN KEY：用一个大号段避免和真实内容撞号 */
    private long probeId() {
        return 900_000_000_000L + PROBE.incrementAndGet();
    }

    private String unique(String prefix) {
        return prefix + "-" + PROBE.incrementAndGet() + "-" + System.nanoTime();
    }

    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    }
}
