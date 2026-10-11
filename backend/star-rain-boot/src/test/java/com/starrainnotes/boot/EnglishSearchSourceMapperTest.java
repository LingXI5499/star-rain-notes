/*
 * EnglishSearchSourceMapperTest：应用启动模块的测试文件。
 * 业务范围：多模块装配、入口路由、安全配置和启动初始化。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：EnglishSearchTypes、EnglishSearchSourceApiAdapter、EnglishSearchSourceMapper。
 * 阅读入口：open（打开英语搜索来源数据映射）；rollback（回滚英语搜索来源数据映射）；everyEnglishTypeCanBeReadAndMapped（every英语类型CanBe读取及Mapped处理入口）；readingIndexRequiresPublicStatusAndClearedExternalRights（阅读索引Requires公开状态及ClearedExternalRights处理入口）。
 */

package com.starrainnotes.boot;

import static org.junit.jupiter.api.Assertions.*;

import com.starrainnotes.english.api.EnglishSearchTypes;
import com.starrainnotes.english.api.impl.EnglishSearchSourceApiAdapter;
import com.starrainnotes.english.knowledge.mapper.EnglishSearchSourceMapper;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.UUID;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/* Real SQL visibility checks; all fixture changes are rolled back. */
/**
 * 英语搜索来源数据映射测试，属于应用启动模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class EnglishSearchSourceMapperTest extends MapperXmlIntegrationSupport {
    /**
     * 当前业务使用的会话对象。
     */
    private SqlSession session;
    /**
     * 数据映射。
     * 协作类型：<code>EnglishSearchSourceMapper</code>。
     */
    private EnglishSearchSourceMapper mapper;
    /**
     * 来源。
     */
    private EnglishSearchSourceApiAdapter source;

    /**
     * 打开英语搜索来源数据映射。
     * 主要协作调用：<code>session.getMapper</code>。
     */
    @BeforeEach
    void open() {
        session = openSession();
        mapper = session.getMapper(EnglishSearchSourceMapper.class);
        source = new EnglishSearchSourceApiAdapter(mapper);
    }

    /**
     * 回滚英语搜索来源数据映射。
     * 主要协作调用：<code>session.rollback</code>、<code>session.close</code>。
     */
    @AfterEach
    void rollback() {
        if (session != null) { session.rollback(true); session.close(); }
    }

    /**
     * every英语类型CanBe读取及Mapped处理入口。
     * 主要协作调用：<code>source.page</code>、<code>source.findPublished</code>。
     */
    @Test
    void everyEnglishTypeCanBeReadAndMapped() {
        for (String type : EnglishSearchTypes.ALL) {
            for (var document : source.page(type, null, 2)) {
                assertEquals(type, document.getContentType());
                assertNotNull(document.getId());
                assertNotNull(document.getTitle());
                assertNotNull(document.getUpdatedAt());
                assertTrue(document.getRoutePath().startsWith("/english/"));
                assertNotNull(source.findPublished(type, document.getId()));
            }
        }
    }

    /**
     * 阅读索引Requires公开状态及ClearedExternalRights处理入口。
     * 主要协作调用：<code>source.findPublished</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test
    void readingIndexRequiresPublicStatusAndClearedExternalRights() throws Exception {
        long id = insert("INSERT INTO sr_english_reading_article(title,slug,summary,body_markdown,translation_zh_markdown,content_origin) VALUES(?,?,?,?,?,?)",
            "Reading search probe", "probe-" + UUID.randomUUID(), "summary", "English body", "中文译文", "EXTERNAL");
        assertNull(source.findPublished("ENGLISH_READING", id));
        execute("UPDATE sr_english_reading_article SET publish_status='PUBLISHED' WHERE id=?", id);
        assertNull(source.findPublished("ENGLISH_READING", id));
        execute("INSERT INTO sr_english_reading_rights(article_id,rights_status,rights_basis) VALUES(?,'CLEARED','fixture permission')", id);
        var document = source.findPublished("ENGLISH_READING", id);
        assertNotNull(document);
        assertTrue(document.getSearchableText().contains("中文译文"));
        assertTrue(document.getRoutePath().startsWith("/english/reading/probe-"));
        execute("UPDATE sr_english_reading_rights SET rights_status='BLOCKED' WHERE article_id=?", id);
        assertNull(source.findPublished("ENGLISH_READING", id));
        execute("UPDATE sr_english_reading_rights SET rights_status='CLEARED' WHERE article_id=?", id);
        execute("UPDATE sr_english_reading_article SET deleted_at=NOW(3) WHERE id=?", id);
        assertNull(source.findPublished("ENGLISH_READING", id));
    }

    /**
     * 写作索引ExcludesPrivate草稿Ineligible及已删除Articles处理入口。
     * 主要协作调用：<code>source.findPublished</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test
    void writingIndexExcludesPrivateDraftIneligibleAndDeletedArticles() throws Exception {
        long id = insert("INSERT INTO sr_english_writing_article(owner_account_id,title,slug,body_markdown,translation_zh_markdown,keywords_json) VALUES(?,?,?,?,?,?)",
            999999L, "Writing search probe", "probe-" + UUID.randomUUID(), "Original body", "原创译文", "[\"keywordprobe\"]");
        assertNull(source.findPublished("ENGLISH_WRITING", id));
        execute("UPDATE sr_english_writing_article SET visibility='PUBLIC',public_eligible=1 WHERE id=?", id);
        assertNull(source.findPublished("ENGLISH_WRITING", id));
        execute("UPDATE sr_english_writing_article SET state='COMPLETED',published_at=NOW(3) WHERE id=?", id);
        var document = source.findPublished("ENGLISH_WRITING", id);
        assertNotNull(document);
        assertNotNull(document.getPublishedAt());
        assertTrue(document.getSearchableText().contains("keywordprobe"));
        assertTrue(document.getSearchableText().contains("原创译文"));
        assertTrue(document.getRoutePath().startsWith("/english/writing/author/"));
        execute("UPDATE sr_english_writing_article SET public_eligible=0 WHERE id=?", id);
        assertNull(source.findPublished("ENGLISH_WRITING", id));
        execute("UPDATE sr_english_writing_article SET public_eligible=1,visibility='PRIVATE' WHERE id=?", id);
        assertNull(source.findPublished("ENGLISH_WRITING", id));
        execute("UPDATE sr_english_writing_article SET visibility='PUBLIC',deleted_at=NOW(3) WHERE id=?", id);
        assertNull(source.findPublished("ENGLISH_WRITING", id));
    }

    /**
     * 词汇Links编码查询参数及孤儿文件WordsAreExcluded处理入口。
     * 主要协作调用：<code>source.findPublished</code>、<code>source.page</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test
    void vocabularyLinksEncodeQueryAndOrphanWordsAreExcluded() throws Exception {
        long theme = insert("INSERT INTO sr_english_vocabulary_theme(layer,name) VALUES('Probe layer','Search probe theme')");
        long word = insert("INSERT INTO sr_english_vocabulary_word(theme_id,word,translation,examples) VALUES(?,?,?,?)",
            theme, "rock & roll", "摇滚", "[]");
        assertEquals("/english/vocabulary/" + theme + "?q=rock+%26+roll",
            source.findPublished("ENGLISH_VOCABULARY_WORD", word).getRoutePath());
        assertEquals(word, source.page("ENGLISH_VOCABULARY_WORD", word - 1, 1).getFirst().getId());
        assertTrue(source.page("ENGLISH_VOCABULARY_WORD", word, 1).stream().noneMatch(row -> row.getId() == word));
        assertEquals("/english/vocabulary/" + theme, source.findPublished("ENGLISH_VOCABULARY_THEME", theme).getRoutePath());
        execute("DELETE FROM sr_english_vocabulary_theme WHERE id=?", theme);
        assertNull(source.findPublished("ENGLISH_VOCABULARY_WORD", word));
    }

    /**
     * 语法Requires已发布Course及Lesson及Existing区块处理入口。
     * 主要协作调用：<code>source.findPublished</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test
    void grammarRequiresPublishedCourseAndLessonAndExistingSection() throws Exception {
        long section = insert("INSERT INTO sr_english_grammar_section(course_id,title,sort_order) VALUES(1,'Search probe',99999)");
        long lesson = insert("INSERT INTO sr_english_grammar_lesson(course_id,section_id,title,slug,body_markdown,sort_order,publish_status) VALUES(1,?,?,?,'Grammar body',99999,'PUBLISHED')",
            section, "Grammar search probe", "probe-" + UUID.randomUUID());
        execute("UPDATE sr_english_grammar_course SET publish_status='WITHDRAWN' WHERE id=1");
        assertNull(source.findPublished("ENGLISH_GRAMMAR_COURSE", 1L));
        assertNull(source.findPublished("ENGLISH_GRAMMAR_LESSON", lesson));
        execute("UPDATE sr_english_grammar_course SET publish_status='PUBLISHED' WHERE id=1");
        assertNotNull(source.findPublished("ENGLISH_GRAMMAR_COURSE", 1L));
        assertNotNull(source.findPublished("ENGLISH_GRAMMAR_LESSON", lesson));
        execute("UPDATE sr_english_grammar_lesson SET publish_status='DRAFT' WHERE id=?", lesson);
        assertNull(source.findPublished("ENGLISH_GRAMMAR_LESSON", lesson));
        execute("UPDATE sr_english_grammar_lesson SET publish_status='PUBLISHED' WHERE id=?", lesson);
        execute("DELETE FROM sr_english_grammar_section WHERE id=?", section);
        assertNull(source.findPublished("ENGLISH_GRAMMAR_LESSON", lesson));
    }

    /**
     * 写入英语搜索来源数据映射。
     * 主要协作调用：<code>session.getConnection</code>。
     *
     * @param sql sql；类型为 <code>String</code>
     * @param values 值集合；类型为 <code>Object[]</code>
     * @return 类型为 <code>long</code> 的处理结果
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    private long insert(String sql, Object... values) throws Exception {
        try (PreparedStatement statement = session.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
            statement.executeUpdate();
            try (var keys = statement.getGeneratedKeys()) { assertTrue(keys.next()); return keys.getLong(1); }
        }
    }

    /**
     * 执行英语搜索来源数据映射。
     * 主要协作调用：<code>session.getConnection</code>、<code>session.clearCache</code>。
     *
     * @param sql sql；类型为 <code>String</code>
     * @param values 值集合；类型为 <code>Object[]</code>
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    private void execute(String sql, Object... values) throws Exception {
        try (PreparedStatement statement = session.getConnection().prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
            statement.executeUpdate();
        }
        session.clearCache();
    }
}
