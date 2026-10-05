package com.starrainnotes.boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.starrainnotes.english.grammar.dto.GrammarDto;
import com.starrainnotes.english.grammar.mapper.GrammarMapper;
import com.starrainnotes.english.listening.dto.ListeningItemDto;
import com.starrainnotes.english.listening.dto.ListeningSegmentDto;
import com.starrainnotes.english.listening.mapper.ListeningItemMapper;
import com.starrainnotes.english.listening.mapper.ListeningSegmentMapper;
import com.starrainnotes.english.reading.dto.ReadingDto;
import com.starrainnotes.english.reading.mapper.ReadingMapper;
import com.starrainnotes.english.vocabulary.dto.VocabularyDto;
import com.starrainnotes.english.vocabulary.dto.VocabularyMemoryEntryRow;
import com.starrainnotes.english.vocabulary.dto.VocabularyMemoryLock;
import com.starrainnotes.english.vocabulary.dto.VocabularyMemoryRow;
import com.starrainnotes.english.vocabulary.dto.VocabularyReviewHistoryRow;
import com.starrainnotes.english.vocabulary.dto.VocabularyReviewLogRow;
import com.starrainnotes.english.vocabulary.dto.VocabularyReviewSchedule;
import com.starrainnotes.english.vocabulary.mapper.VocabularyMapper;
import com.starrainnotes.english.vocabulary.mapper.VocabularyStudyMapper;
import com.starrainnotes.english.vocabulary.vo.VocabularyStudySettingsVO;
import com.starrainnotes.english.vocabulary.vo.VocabularySummaryVO;
import com.starrainnotes.english.vocabulary.vo.VocabularyWordAudioVO;
import com.starrainnotes.english.writing.dto.WritingPromptDto;
import com.starrainnotes.english.writing.dto.WritingResourceDto;
import com.starrainnotes.english.writing.mapper.WritingPromptMapper;
import com.starrainnotes.english.writing.mapper.WritingResourceMapper;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/*
 * english 模块 8 个 Mapper 的真实数据库读写验证。
 *
 * 这 8 个 Mapper 有一个共同特征：**几乎每条查询都用 <resultMap> 把列名翻译成 DTO 字段**
 * （transcript_markdown → bodyMarkdown、listening_level → difficultyLevel、reading_level →
 * difficultyLevel、background_markdown → bodyMarkdown、is_primary → primary ……）。
 * resultMap 里少写一行 <result>，属性就是 null，没有任何类型检查会发现。所以本类对每条
 * 代表性的读路径都逐字段断言，而不是只断言「不抛异常」。
 *
 * 另外三处容易出错的地方也钉住了：
 *   1. `listForPublic` / `publicBySlug` / `publicLesson` 把 publish_status='PUBLISHED' 写死在 SQL 里；
 *   2. 六条 setStatus 都用 `published_at = COALESCE(published_at, NOW(3))`，重新发布不能改写成「今天」；
 *      published_at 不在任何 resultMap 里，所以用一条 SELECT 直接核对；
 *   3. vocabulary 的 updateWord 用 COALESCE 保住 scene_meaning / inflections（不传=保留旧值），
 *      而 startMemory / importMemory 是「只增不减」的 UPSERT。
 *
 * sr_english_vocabulary_word_audio 在全库是空表，而且没有任何写入语句（V1 导入数据），
 * 所以 audios 的验证在同一个未提交事务里用一条 INSERT 造行，再走 resultMap 读回来。
 *
 * 全程在一个不 commit 的 SqlSession 里跑，@AfterEach 回滚；最后一个用例用新连接复核。
 */
class EnglishMapperXmlTest extends MapperXmlIntegrationSupport {

    private static final AtomicLong PROBE = new AtomicLong(System.nanoTime() % 1_000_000L);

    private SqlSession session;
    private GrammarMapper grammar;
    private ListeningItemMapper listeningItems;
    private ListeningSegmentMapper listeningSegments;
    private ReadingMapper readings;
    private VocabularyMapper vocabulary;
    private VocabularyStudyMapper vocabularyStudy;
    private WritingPromptMapper writingPrompts;
    private WritingResourceMapper writingResources;

    @BeforeEach
    void open() {
        session = openSession();
        grammar = session.getMapper(GrammarMapper.class);
        listeningItems = session.getMapper(ListeningItemMapper.class);
        listeningSegments = session.getMapper(ListeningSegmentMapper.class);
        readings = session.getMapper(ReadingMapper.class);
        vocabulary = session.getMapper(VocabularyMapper.class);
        vocabularyStudy = session.getMapper(VocabularyStudyMapper.class);
        writingPrompts = session.getMapper(WritingPromptMapper.class);
        writingResources = session.getMapper(WritingResourceMapper.class);
    }

    @AfterEach
    void rollback() {
        if (session != null) {
            session.rollback();
            session.close();
        }
    }

    @Test
    void everyEnglishMapperIsReachableThroughTheSession() {
        assertNotNull(grammar);
        assertNotNull(listeningItems);
        assertNotNull(listeningSegments);
        assertNotNull(readings);
        assertNotNull(vocabulary);
        assertNotNull(vocabularyStudy);
        assertNotNull(writingPrompts);
        assertNotNull(writingResources);
    }

    @Test
    void grammarCourseRoundTripsAndKeepsItsFirstPublishedAt() {
        GrammarDto.Course original = grammar.course();
        assertNotNull(original, "sr_english_grammar_course 没有 id = 1 的行，检查 V2_020 迁移");
        assertNotNull(original.getPublishStatus());

        String marker = unique("mapperxmlgrammar");
        GrammarDto.Course edited = grammar.course();
        edited.setTitle(marker);
        edited.setSubtitle(marker + "-副标题");
        edited.setSummary(marker + "-摘要");
        edited.setIntroduction(marker + "-介绍");
        edited.setRoadmapMarkdown(marker + "-路线图");
        assertEquals(1, grammar.updateCourse(edited));

        GrammarDto.Course updated = grammar.course();
        assertEquals(marker, updated.getTitle(), "resultMap 的 title 没生效");
        assertEquals(marker + "-副标题", updated.getSubtitle(), "resultMap 的 subtitle 没生效");
        assertEquals(marker + "-摘要", updated.getSummary(), "resultMap 的 summary 没生效");
        assertEquals(marker + "-介绍", updated.getIntroduction(), "resultMap 的 introduction 没生效");
        assertEquals(marker + "-路线图", updated.getRoadmapMarkdown(),
                "别名 roadmapMarkdown 丢失（XML 映射的是 roadmap_markdown）");
        assertEquals(original.getPublishStatus(), updated.getPublishStatus(),
                "改文案不应改动 publish_status");

        assertEquals(1, grammar.setCourseStatus("PUBLISHED"));
        assertEquals("PUBLISHED", grammar.course().getPublishStatus(),
                "别名 publishStatus 丢失（XML 映射的是 publish_status）");
        Object firstPublishedAt = publishedAtOf("sr_english_grammar_course", 1L);
        assertNotNull(firstPublishedAt, "setCourseStatus 必须写入 published_at");

        assertEquals(1, grammar.setCourseStatus("DRAFT"));
        assertEquals("DRAFT", grammar.course().getPublishStatus());
        assertEquals(1, grammar.setCourseStatus("PUBLISHED"));
        assertEquals(firstPublishedAt, publishedAtOf("sr_english_grammar_course", 1L),
                "重新发布把首次发布时间改写了（COALESCE 丢失）");
    }

    @Test
    void grammarSectionAndLessonCrudRoundTrips() {
        GrammarDto.Section section = new GrammarDto.Section();
        section.setTitle("XML 验证语法章节");
        section.setSortOrder(4242);
        assertEquals(1, grammar.insertSection(section));
        assertNotNull(section.getId(), "insertSection 没有回填自增主键");

        GrammarDto.Section loaded = grammar.section(section.getId());
        assertNotNull(loaded);
        assertEquals("XML 验证语法章节", loaded.getTitle(), "resultMap 的 title 没生效");
        assertEquals(4242, loaded.getSortOrder(), "resultMap 的 sortOrder 没生效");
        assertTrue(grammar.sections().stream().anyMatch(item -> item.getId().equals(section.getId())),
                "sections() 没有带回刚插入的章节");
        assertNull(grammar.section(-1L), "不存在的章节应返回 null");

        String slug = unique("mapperxmlgrammarlesson");
        GrammarDto.Lesson lesson = newLesson(section.getId(), slug);
        assertEquals(1, grammar.insertLesson(lesson));
        assertNotNull(lesson.getId(), "insertLesson 没有回填自增主键");

        GrammarDto.Lesson stored = grammar.lessonById(lesson.getId());
        assertNotNull(stored);
        assertEquals(section.getId(), stored.getSectionId(), "别名 sectionId 丢失（XML 映射的是 section_id）");
        assertEquals(slug, stored.getSlug());
        assertEquals("XML 验证课时", stored.getTitle());
        assertEquals("XML 验证课时摘要", stored.getSummary());
        assertEquals("# XML 验证课时正文", stored.getBodyMarkdown(),
                "别名 bodyMarkdown 丢失（XML 映射的是 body_markdown）");
        assertEquals("DRAFT", stored.getPublishStatus(),
                "insertLesson 不写 publish_status，应当取列默认值 DRAFT");
        assertEquals(0, stored.getSortOrder());
        assertNull(grammar.publicLesson(slug), "草稿课时不应能按 slug 公开读取");

        assertEquals(1, grammar.lessons(section.getId(), false).size());
        assertTrue(grammar.lessons(section.getId(), true).isEmpty(), "publicOnly 过滤没生效");

        GrammarDto.Lesson second = newLesson(section.getId(), unique("mapperxmlgrammarlesson"));
        assertEquals(1, grammar.insertLesson(second));
        assertEquals(2, grammar.lessons(section.getId(), false).size());
        assertTrue(grammar.lessons(section.getId(), true).isEmpty());

        assertEquals(1, grammar.setLessonStatus(lesson.getId(), "PUBLISHED"));
        GrammarDto.Lesson published = grammar.lessonById(lesson.getId());
        assertEquals("PUBLISHED", published.getPublishStatus());
        assertNotNull(grammar.publicLesson(slug), "已发布课时应当能按 slug 公开读取");
        assertEquals(1, grammar.lessons(section.getId(), true).size(),
                "publicOnly 应当只放行已发布的那一条");
        Object firstPublishedAt = publishedAtOf("sr_english_grammar_lesson", lesson.getId());
        assertNotNull(firstPublishedAt, "setLessonStatus 必须写入 published_at");

        // updateLesson 不改 publish_status：改文案不应把已发布课时打回草稿
        published.setTitle("改名后的课时");
        published.setSummary(null);
        published.setBodyMarkdown("# 新正文");
        published.setSortOrder(7);
        assertEquals(1, grammar.updateLesson(published));
        GrammarDto.Lesson renamed = grammar.lessonById(lesson.getId());
        assertEquals("改名后的课时", renamed.getTitle());
        assertNull(renamed.getSummary(), "summary 传 null 没有被清空（这条语句是普通 SET）");
        assertEquals("# 新正文", renamed.getBodyMarkdown());
        assertEquals(7, renamed.getSortOrder());
        assertEquals("PUBLISHED", renamed.getPublishStatus(), "改文案不应改动 publish_status");
        assertNotNull(grammar.publicLesson(slug));

        assertEquals(1, grammar.setLessonStatus(lesson.getId(), "DRAFT"));
        assertEquals(1, grammar.setLessonStatus(lesson.getId(), "PUBLISHED"));
        assertEquals(firstPublishedAt, publishedAtOf("sr_english_grammar_lesson", lesson.getId()),
                "重新发布把首次发布时间改写了（COALESCE 丢失）");

        assertEquals(1, grammar.deleteLesson(second.getId()));
        assertNull(grammar.lessonById(second.getId()), "已删除的课时不应还能读到");
        assertEquals(0, grammar.deleteLesson(second.getId()));
        assertEquals(1, grammar.deleteLesson(lesson.getId()));
        assertEquals(1, grammar.deleteSection(section.getId()));
        assertNull(grammar.section(section.getId()));
        assertEquals(0, grammar.deleteSection(section.getId()));
    }

    @Test
    void listeningItemCrudHonoursPublicAndSearchFilters() {
        String search = unique("mapperxmllistening");
        ListeningItemDto.Item item = newItem(search);
        assertEquals(1, listeningItems.insert(item));
        assertNotNull(item.getId(), "insert 没有回填自增主键");

        ListeningItemDto.Item stored = listeningItems.byId(item.getId());
        assertNotNull(stored, "byId 读不到刚插入的行");
        assertEquals(item.getSlug(), stored.getSlug());
        assertEquals(search, stored.getTitle());
        assertEquals("XML 验证听力摘要", stored.getSummary());
        assertEquals("# XML 验证听力原文", stored.getBodyMarkdown(),
                "别名 bodyMarkdown 丢失（XML 映射的是 transcript_markdown）");
        assertEquals("B1", stored.getCefrLevel(), "别名 cefrLevel 丢失（XML 映射的是 cefr_level）");
        assertEquals(3, stored.getDifficultyLevel().intValue(),
                "别名 difficultyLevel 丢失（XML 映射的是 listening_level）");
        assertEquals(item.getAudioMediaId(), stored.getAudioMediaId(), "别名 audioMediaId 丢失");
        assertEquals(300, stored.getDurationSeconds().intValue(), "别名 durationSeconds 丢失");
        assertEquals("XML 验证来源", stored.getSourceName(), "别名 sourceName 丢失");
        assertEquals("https://example.test/listening", stored.getSourceUrl(), "别名 sourceUrl 丢失");
        assertEquals("DRAFT", stored.getPublishStatus(),
                "insert 不写 publish_status，应当取列默认值 DRAFT");
        assertEquals(0, stored.getSortOrder());

        assertNull(listeningItems.byId(-1L));
        assertNull(listeningItems.publicBySlug(item.getSlug()), "草稿听力不应能按 slug 公开读取");
        assertEquals(1, listeningItems.count(false, search), "search 过滤没生效");
        assertEquals(0, listeningItems.count(true, search), "publicOnly 过滤没生效");
        assertEquals(0, listeningItems.count(false, search + "-missing"));
        assertTrue(listeningItems.count(false, null) >= 1, "search 为 null 时必须退化成统计全部");

        List<ListeningItemDto.Item> list = listeningItems.list(false, search, 0, 10);
        assertEquals(1, list.size());
        assertEquals(item.getId(), list.get(0).getId());
        assertEquals(1, listeningItems.list(false, search, 0, 1).size(), "LIMIT 没生效");
        assertTrue(listeningItems.list(false, search, 1, 10).isEmpty(), "OFFSET 没生效");
        assertTrue(listeningItems.list(true, search, 0, 10).isEmpty());

        assertEquals(1, listeningItems.setStatus(item.getId(), "PUBLISHED"));
        assertEquals("PUBLISHED", listeningItems.byId(item.getId()).getPublishStatus());
        assertNotNull(listeningItems.publicBySlug(item.getSlug()), "已发布听力应当能按 slug 公开读取");
        assertEquals(1, listeningItems.count(true, search));
        Object firstPublishedAt = publishedAtOf("sr_english_listening_item", item.getId());
        assertNotNull(firstPublishedAt, "setStatus 必须写入 published_at");
        assertEquals(1, listeningItems.setStatus(item.getId(), "DRAFT"));
        assertEquals(1, listeningItems.setStatus(item.getId(), "PUBLISHED"));
        assertEquals(firstPublishedAt, publishedAtOf("sr_english_listening_item", item.getId()),
                "重新发布把首次发布时间改写了（COALESCE 丢失）");

        // update 不改 publish_status，也不该动 slug 之外的身份列
        // summary 是 NOT NULL 列，这里改成测试可空列 source_name 的「传 null 即清空」语义
        ListeningItemDto.Item edited = listeningItems.byId(item.getId());
        edited.setTitle(search + "-改名");
        edited.setSourceName(null);
        edited.setDifficultyLevel(5);
        edited.setDurationSeconds(600);
        assertEquals(1, listeningItems.update(edited));
        ListeningItemDto.Item afterUpdate = listeningItems.byId(item.getId());
        assertEquals(search + "-改名", afterUpdate.getTitle());
        assertNull(afterUpdate.getSourceName(), "source_name 传 null 没有被清空（这条语句是普通 SET）");
        assertEquals(5, afterUpdate.getDifficultyLevel().intValue());
        assertEquals(600, afterUpdate.getDurationSeconds().intValue());
        assertEquals("PUBLISHED", afterUpdate.getPublishStatus(), "改内容不应改动 publish_status");
        assertEquals(0, listeningItems.update(withItemId(-1L, edited)));

        assertEquals(1, listeningItems.delete(item.getId()));
        assertNull(listeningItems.byId(item.getId()));
        assertEquals(0, listeningItems.delete(item.getId()));
    }

    @Test
    void listeningSegmentsFollowTheirItemAndOrdering() {
        ListeningItemDto.Item item = insertPublishedItem();
        ListeningItemDto.Item other = insertPublishedItem();

        ListeningSegmentDto.Segment later = newSegment(item.getId(), 2, 2000, "第二句");
        assertEquals(1, listeningSegments.insert(later));
        assertNotNull(later.getId(), "insert 没有回填自增主键");
        ListeningSegmentDto.Segment earlier = newSegment(item.getId(), 1, 1000, "第一句");
        assertEquals(1, listeningSegments.insert(earlier));
        assertEquals(1, listeningSegments.insert(newSegment(other.getId(), 1, 1000, "别的听力")));

        List<ListeningSegmentDto.Segment> forAdmin = listeningSegments.listForAdmin(item.getId());
        assertEquals(2, forAdmin.size(), "只应返回当前听力的切片");
        assertEquals(earlier.getId(), forAdmin.get(0).getId(), "切片必须按 sort_order 升序");
        assertEquals(later.getId(), forAdmin.get(1).getId());

        ListeningSegmentDto.Segment loaded = forAdmin.get(0);
        assertEquals(item.getId(), loaded.getListeningItemId(),
                "别名 listeningItemId 丢失（XML 映射的是 listening_item_id）");
        assertEquals(1000, loaded.getStartMs(), "别名 startMs 丢失");
        assertEquals(1500, loaded.getEndMs(), "别名 endMs 丢失");
        assertEquals("第一句", loaded.getTranscriptText(), "别名 transcriptText 丢失");
        assertEquals("XML 验证翻译", loaded.getTranslationText(), "别名 translationText 丢失");
        assertEquals(1, loaded.getSortOrder(), "别名 sortOrder 丢失");

        List<ListeningSegmentDto.Segment> forPublic = listeningSegments.listForPublic(item.getSlug());
        assertEquals(2, forPublic.size(), "已发布听力的切片应当能公开读取");
        assertEquals(earlier.getId(), forPublic.get(0).getId());
        assertEquals(other.getId(), listeningSegments.listForAdmin(other.getId()).get(0).getListeningItemId());

        assertEquals(earlier.getId(), listeningSegments.get(item.getId(), earlier.getId()).getId());
        assertNull(listeningSegments.get(other.getId(), earlier.getId()),
                "get 必须同时校验 listening_item_id，否则能跨听力读到别人的切片");
        assertNull(listeningSegments.get(item.getId(), probeId()));

        ListeningSegmentDto.Segment edited = listeningSegments.get(item.getId(), earlier.getId());
        edited.setTranscriptText("改后的第一句");
        edited.setEndMs(1800);
        assertEquals(1, listeningSegments.update(edited));
        ListeningSegmentDto.Segment afterUpdate = listeningSegments.get(item.getId(), earlier.getId());
        assertEquals("改后的第一句", afterUpdate.getTranscriptText());
        assertEquals(1800, afterUpdate.getEndMs());
        assertEquals(item.getId(), afterUpdate.getListeningItemId(), "更新切片不应改动归属听力");
        assertEquals(0, listeningSegments.update(withSegmentItemId(edited, other.getId())),
                "update 的 WHERE 带 listening_item_id，归属不对时应当 0 行");

        assertEquals(0, listeningSegments.delete(other.getId(), earlier.getId()),
                "delete 的 WHERE 带 listening_item_id，归属不对时不应删掉");
        assertEquals(1, listeningSegments.delete(item.getId(), earlier.getId()));
        assertEquals(0, listeningSegments.delete(item.getId(), earlier.getId()));
        assertEquals(1, listeningSegments.listForAdmin(item.getId()).size());
        assertEquals(1, listeningItems.deleteSegments(item.getId()), "应当删掉剩下那一条切片");
        assertEquals(0, listeningItems.deleteSegments(item.getId()));
        assertTrue(listeningSegments.listForAdmin(item.getId()).isEmpty());
    }

    @Test
    void readingArticleCrudHonoursPublicAndSearchFilters() {
        String search = unique("mapperxmlreading");
        ReadingDto.Article article = newArticle(search);
        assertEquals(1, readings.insert(article));
        assertNotNull(article.getId(), "insert 没有回填自增主键");

        ReadingDto.Article stored = readings.byId(article.getId());
        assertNotNull(stored, "byId 读不到刚插入的行");
        assertEquals(article.getSlug(), stored.getSlug());
        assertEquals(search, stored.getTitle());
        assertEquals("XML 验证阅读摘要", stored.getSummary());
        assertEquals("# XML 验证阅读正文", stored.getBodyMarkdown(),
                "别名 bodyMarkdown 丢失（XML 映射的是 body_markdown）");
        assertEquals("XML 验证来源", stored.getSourceName(), "别名 sourceName 丢失");
        assertEquals("https://example.test/reading", stored.getSourceUrl(), "别名 sourceUrl 丢失");
        assertEquals("A2", stored.getCefrLevel());
        assertEquals(4, stored.getDifficultyLevel().intValue(),
                "别名 difficultyLevel 丢失（XML 映射的是 reading_level）");
        assertEquals("DRAFT", stored.getPublishStatus(),
                "insert 不写 publish_status，应当取列默认值 DRAFT");
        assertEquals(0, stored.getSortOrder());

        assertNull(readings.byId(-1L));
        assertNull(readings.publicBySlug(article.getSlug()), "草稿文章不应能按 slug 公开读取");
        assertEquals(1, readings.count(false, search), "search 过滤没生效");
        assertEquals(0, readings.count(true, search), "publicOnly 过滤没生效");
        assertEquals(0, readings.count(false, search + "-missing"));
        assertTrue(readings.count(false, null) >= 1, "search 为 null 时必须退化成统计全部");
        assertEquals(1, readings.list(false, search, 0, 10).size());
        assertEquals(1, readings.list(false, search, 0, 1).size(), "LIMIT 没生效");
        assertTrue(readings.list(false, search, 1, 10).isEmpty(), "OFFSET 没生效");

        assertEquals(1, readings.setStatus(article.getId(), "PUBLISHED"));
        assertEquals("PUBLISHED", readings.byId(article.getId()).getPublishStatus());
        assertNotNull(readings.publicBySlug(article.getSlug()), "已发布文章应当能按 slug 公开读取");
        assertEquals(1, readings.count(true, search));
        Object firstPublishedAt = publishedAtOf("sr_english_reading_article", article.getId());
        assertNotNull(firstPublishedAt, "setStatus 必须写入 published_at");
        assertEquals(1, readings.setStatus(article.getId(), "DRAFT"));
        assertEquals(1, readings.setStatus(article.getId(), "PUBLISHED"));
        assertEquals(firstPublishedAt, publishedAtOf("sr_english_reading_article", article.getId()),
                "重新发布把首次发布时间改写了（COALESCE 丢失）");

        // summary 是 NOT NULL 列，所以这里用可空列 source_name 验证「传 null 即清空」
        ReadingDto.Article edited = readings.byId(article.getId());
        edited.setTitle(search + "-改名");
        edited.setSourceName(null);
        edited.setDifficultyLevel(6);
        edited.setSlug(article.getSlug() + "-renamed");
        assertEquals(1, readings.update(edited));
        ReadingDto.Article afterUpdate = readings.byId(article.getId());
        assertEquals(search + "-改名", afterUpdate.getTitle());
        assertNull(afterUpdate.getSourceName(), "source_name 传 null 没有被清空（这条语句是普通 SET）");
        assertEquals(6, afterUpdate.getDifficultyLevel().intValue());
        assertEquals(article.getSlug() + "-renamed", afterUpdate.getSlug());
        assertEquals("PUBLISHED", afterUpdate.getPublishStatus(), "改内容不应改动 publish_status");
        assertNotNull(readings.publicBySlug(article.getSlug() + "-renamed"), "改 slug 后应当按新地址公开");
        assertNull(readings.publicBySlug(article.getSlug()), "旧地址应当查不到了");

        assertEquals(1, readings.delete(article.getId()));
        assertNull(readings.byId(article.getId()));
        assertEquals(0, readings.delete(article.getId()));
    }

    @Test
    void vocabularyThemeAndWordCrudRoundTrips() {
        VocabularyDto.Theme theme = newTheme();
        assertEquals(1, vocabulary.insertTheme(theme));
        assertNotNull(theme.getId(), "insertTheme 没有回填自增主键");

        VocabularyDto.Theme loadedTheme = vocabulary.theme(theme.getId());
        assertNotNull(loadedTheme, "theme 读不到刚插入的主题");
        assertEquals(theme.getName(), loadedTheme.getName(), "resultMap 的 name 没生效");
        assertEquals(theme.getLayer(), loadedTheme.getLayer());
        assertEquals(theme.getLayerOrder(), loadedTheme.getLayerOrder(),
                "别名 layerOrder 丢失（XML 映射的是 layer_order）");
        assertEquals(theme.getSortOrder(), loadedTheme.getSortOrder());
        assertEquals(0, loadedTheme.getWordCount(), "还没有单词时 word_count 必须是 0");
        assertTrue(vocabulary.themes().stream().anyMatch(item -> item.getId().equals(theme.getId())),
                "themes() 没有带回刚插入的主题");
        assertNull(vocabulary.theme(-1L), "不存在的主题应返回 null");

        VocabularyDto.Word word = newWord(theme.getId(), unique("mapperxmlword"));
        assertEquals(1, vocabulary.insertWord(word));
        assertNotNull(word.getId(), "insertWord 没有回填自增主键");

        VocabularyDto.Word stored = vocabulary.word(word.getId());
        assertNotNull(stored);
        assertEquals(theme.getId(), stored.getThemeId(), "别名 themeId 丢失（XML 映射的是 theme_id）");
        assertEquals(word.getWord(), stored.getWord());
        assertEquals("n.", stored.getPartOfSpeech(), "别名 partOfSpeech 丢失");
        assertEquals("/ˈtest/", stored.getPhoneticUs(), "别名 phoneticUs 丢失");
        assertEquals("/ˈtest-uk/", stored.getPhoneticUk(), "别名 phoneticUk 丢失");
        assertEquals("XML 验证释义", stored.getTranslation());
        assertEquals("XML 验证本主题用法", stored.getSceneMeaning(), "别名 sceneMeaning 丢失");
        assertEquals("mapperxml-inflection", stored.getInflections());
        assertTrue(stored.getExamples().contains("XML verify"),
                "examples 是 JSON 列，XML 用 CAST(examples AS CHAR) 读成字符串");
        assertEquals(5, stored.getSortOrder());
        assertEquals(0, stored.getMemoryCount());
        assertEquals(1, vocabulary.theme(theme.getId()).getWordCount(),
                "word_count 聚合没有把新词算进去");

        assertEquals(1, vocabulary.countWords(theme.getId(), null));
        assertEquals(1, vocabulary.countWords(theme.getId(), word.getWord()), "search 过滤没生效");
        assertEquals(1, vocabulary.countWords(theme.getId(), "XML 验证释义"),
                "search 必须同时匹配 word 与 translation");
        assertEquals(0, vocabulary.countWords(theme.getId(), "no-such-word"));
        assertEquals(0, vocabulary.countWords(probeId(), null), "themeId 过滤没生效");
        assertEquals(1, vocabulary.words(theme.getId(), null, 0, 10).size());
        assertEquals(1, vocabulary.words(theme.getId(), word.getWord(), 0, 10).size());
        assertEquals(1, vocabulary.words(theme.getId(), null, 0, 1).size(), "LIMIT 没生效");
        assertTrue(vocabulary.words(theme.getId(), null, 1, 10).isEmpty(), "OFFSET 没生效");
        assertTrue(vocabulary.words(probeId(), null, 0, 10).isEmpty());
        assertEquals(10, vocabulary.words(null, null, 0, 10).size(),
                "themeId 为 null 时必须退化成不过滤主题，只受 LIMIT 约束");
        assertEquals(1, vocabulary.wordsByIds(List.of(word.getId())).size());
        assertEquals(word.getId(), vocabulary.wordsByIds(List.of(word.getId())).get(0).getId());

        // updateWord 用 COALESCE 保住 scene_meaning / inflections：不传 null 就要保留旧值
        VocabularyDto.Word update = vocabulary.word(word.getId());
        update.setTranslation("改后的释义");
        update.setSceneMeaning(null);
        update.setInflections(null);
        assertEquals(1, vocabulary.updateWord(update));
        VocabularyDto.Word kept = vocabulary.word(word.getId());
        assertEquals("改后的释义", kept.getTranslation());
        assertEquals("XML 验证本主题用法", kept.getSceneMeaning(),
                "scene_meaning 传 null 被清空了，COALESCE 更新语义在迁移中丢了");
        assertEquals("mapperxml-inflection", kept.getInflections(),
                "inflections 传 null 被清空了，COALESCE 更新语义在迁移中丢了");

        VocabularyDto.Word overwrite = vocabulary.word(word.getId());
        overwrite.setSceneMeaning("新用法");
        overwrite.setInflections("新词形");
        assertEquals(1, vocabulary.updateWord(overwrite));
        assertEquals("新用法", vocabulary.word(word.getId()).getSceneMeaning());
        assertEquals("新词形", vocabulary.word(word.getId()).getInflections());
        assertEquals("改后的释义", vocabulary.word(word.getId()).getTranslation(), "改用法不应动释义");
        assertEquals(0, vocabulary.updateWord(withWordId(-1L, overwrite)));

        // audios 的数据由 V1 导入，Mapper 里没有写入语句：在同一个未提交事务里造一行再读回
        long mediaAssetId = probeId();
        long audioId = insertAudioFixture(word.getId(), mediaAssetId);
        List<VocabularyWordAudioVO> audios = vocabulary.audios(List.of(word.getId()));
        assertEquals(1, audios.size(), "audios 没有读到刚造的授权发音");
        VocabularyWordAudioVO audio = audios.get(0);
        assertEquals(audioId, audio.getId());
        assertEquals(word.getId(), audio.getWordId(), "别名 wordId 丢失（XML 映射的是 word_id）");
        assertEquals("US", audio.getAccent());
        assertEquals(Long.valueOf(mediaAssetId), audio.getMediaAssetId(), "别名 mediaAssetId 丢失");
        assertEquals("UPLOADED", audio.getProvider());
        assertEquals("https://example.test/audio.mp3", audio.getSourceUrl(), "别名 sourceUrl 丢失");
        assertEquals("XML 验证授权说明", audio.getLicenseNote(), "别名 licenseNote 丢失");
        assertTrue(audio.isPrimary(), "别名 primary 丢失（XML 映射的是 is_primary）");
        assertTrue(vocabulary.audios(List.of(probeId())).isEmpty());

        assertEquals(1, vocabulary.deleteWord(word.getId()));
        assertNull(vocabulary.word(word.getId()));
        assertEquals(1, vocabulary.deleteTheme(theme.getId()));
        assertNull(vocabulary.theme(theme.getId()));
        assertEquals(0, vocabulary.deleteTheme(theme.getId()));
    }

    @Test
    void vocabularyStudySettingsAndSingleCardPreferenceUpsert() {
        long accountId = probeId();
        assertNull(vocabularyStudy.settings(accountId), "还没有设置行时应当返回 null");

        VocabularyStudySettingsVO settings = new VocabularyStudySettingsVO();
        settings.setShowEnglish(true);
        settings.setShowChinese(false);
        settings.setReviewDirection("EN_TO_ZH");
        settings.setDailyNewLimit(15);
        settings.setDailyReviewLimit(120);
        assertEquals(1, vocabularyStudy.upsertSettings(accountId, settings), "首次 upsert 应当插入一行");

        VocabularyStudySettingsVO stored = vocabularyStudy.settings(accountId);
        assertNotNull(stored, "settings 读不到刚写入的设置");
        assertTrue(stored.isShowEnglish(), "别名 showEnglish 丢失（XML 映射的是 show_english）");
        assertFalse(stored.isShowChinese(), "别名 showChinese 丢失");
        assertEquals("EN_TO_ZH", stored.getReviewDirection(),
                "别名 reviewDirection 丢失（XML 映射的是 review_direction）");
        assertEquals(15, stored.getDailyNewLimit(), "别名 dailyNewLimit 丢失");
        assertEquals(120, stored.getDailyReviewLimit(), "别名 dailyReviewLimit 丢失");

        settings.setReviewDirection("MIXED");
        settings.setDailyNewLimit(30);
        assertEquals(2, vocabularyStudy.upsertSettings(accountId, settings),
                "ON DUPLICATE KEY UPDATE 真正改动行时 MySQL 返回 2");
        VocabularyStudySettingsVO overwritten = vocabularyStudy.settings(accountId);
        assertEquals("MIXED", overwritten.getReviewDirection(), "已有设置没有被覆盖");
        assertEquals(30, overwritten.getDailyNewLimit());
        assertEquals(120, overwritten.getDailyReviewLimit(), "未被改动的列应当保持原值");

        long wordId = insertProbeWord(insertProbeTheme().getId()).getId();
        assertNull(vocabularyStudy.displayMode(accountId, wordId), "还没有偏好时应当返回 null");
        assertEquals(1, vocabularyStudy.upsertDisplay(accountId, wordId, "ENGLISH_ONLY"));
        assertEquals("ENGLISH_ONLY", vocabularyStudy.displayMode(accountId, wordId));
        assertEquals(2, vocabularyStudy.upsertDisplay(accountId, wordId, "BILINGUAL"),
                "改动已有偏好时 ON DUPLICATE KEY UPDATE 返回 2");
        assertEquals("BILINGUAL", vocabularyStudy.displayMode(accountId, wordId));
        assertEquals(1, vocabularyStudy.deleteDisplay(accountId, wordId));
        assertEquals(0, vocabularyStudy.deleteDisplay(accountId, wordId));
        assertNull(vocabularyStudy.displayMode(accountId, wordId));
    }

    @Test
    void vocabularyMemoryLifecycleFollowsItsGuards() {
        long accountId = probeId();
        VocabularyDto.Theme theme = insertProbeTheme();
        VocabularyDto.Word word = insertProbeWord(theme.getId());
        VocabularyDto.Word other = insertProbeWord(theme.getId());
        long wordId = word.getId();
        long otherWordId = other.getId();
        LocalDateTime now = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);

        assertEquals(1, vocabularyStudy.wordExists(wordId), "wordExists 没生效");
        assertEquals(0, vocabularyStudy.wordExists(probeId()));
        assertNull(vocabularyStudy.memory(accountId, wordId), "还没有记忆行时应当返回 null");
        assertTrue(vocabularyStudy.memorySnapshot(accountId).isEmpty());
        assertEquals(0L, vocabularyStudy.totalMemoryCount(accountId));
        assertEquals(0L, vocabularyStudy.activeCount(accountId));

        assertEquals(1, vocabularyStudy.startMemory(accountId, wordId, now));
        VocabularyMemoryRow memory = vocabularyStudy.memory(accountId, wordId);
        assertNotNull(memory, "memory 读不到刚加入计划的词");
        assertEquals(wordId, memory.getWordId(), "别名 wordId 丢失（XML 映射的是 m.word_id）");
        assertEquals(0, memory.getMemoryCount());
        assertEquals(0, memory.getReviewStep(), "别名 reviewStep 丢失（XML 映射的是 review_step）");
        assertEquals(0, memory.getReviewCount(), "别名 reviewCount 丢失");
        assertEquals(now, memory.getFirstLearnedAt(), "startMemory 必须写入首次学习时间");
        assertNull(memory.getLastReviewedAt());
        assertEquals(now, memory.getNextReviewAt(), "别名 nextReviewAt 丢失");
        assertEquals("ACTIVE", memory.getLearningStatus(), "别名 learningStatus 丢失");
        assertNull(memory.getDisplayMode(), "还没有单卡偏好时 displayMode 应当是 null");

        // totalMemoryCount 是 SUM(memory_count)：刚加入计划的词 memory_count 还是 0
        assertEquals(0L, vocabularyStudy.totalMemoryCount(accountId));
        assertEquals(1L, vocabularyStudy.activeCount(accountId));
        assertEquals(1, vocabularyStudy.introducedSince(accountId, now.minusMinutes(1)),
                "introducedSince 没生效");
        assertEquals(0, vocabularyStudy.introducedSince(accountId, now.plusMinutes(1)),
                "first_learned_at 在窗口之后时不应被统计");
        assertEquals(1L, vocabularyStudy.dueCount(accountId, now), "next_review_at <= now 应当算到期");
        assertEquals(0L, vocabularyStudy.dueCount(accountId, now.minusMinutes(1)));
        assertEquals(List.of(wordId), vocabularyStudy.dueWordIds(accountId, now, 10),
                "dueWordIds 没生效");
        assertEquals(1, vocabularyStudy.dueWordIds(accountId, now, 1).size(), "LIMIT 没生效");

        List<VocabularyMemoryRow> rows = vocabularyStudy.memories(accountId, List.of(wordId, otherWordId));
        assertEquals(2, rows.size(), "memories 以词表左连接，没有记忆行的词也必须返回一行");
        assertEquals(wordId, rows.get(0).getWordId(), "别名 wordId 丢失");
        assertEquals(otherWordId, rows.get(1).getWordId());
        assertEquals(0, rows.get(1).getMemoryCount(), "没有记忆行时 COALESCE 应当给 0");
        assertEquals(0, rows.get(1).getReviewStep());
        assertEquals(0, rows.get(1).getReviewCount());
        assertNull(rows.get(1).getLearningStatus(), "没有记忆行时 learningStatus 应当是 null");
        assertNull(rows.get(1).getNextReviewAt());

        List<VocabularyMemoryEntryRow> snapshot = vocabularyStudy.memorySnapshot(accountId);
        assertEquals(1, snapshot.size(), "memorySnapshot 应当只返回本账户的记忆行");
        assertEquals(wordId, snapshot.get(0).getWordId(), "别名 wordId 丢失");
        assertEquals(0, snapshot.get(0).getMemoryCount(), "别名 memoryCount 丢失");
        assertNull(snapshot.get(0).getLastMemoryAt(), "还没有复习过时 last_memory_at 应当是 null");

        VocabularySummaryVO summary = vocabularyStudy.summary(accountId, now, 10);
        assertNotNull(summary, "summary 必须返回一行（聚合查询）");
        assertEquals(1L, summary.getTotal(), "别名 total 丢失");
        assertEquals(0L, summary.getCompleted(), "还没走完十档间隔，completed 应当是 0");
        assertEquals(1L, summary.getInProgress(), "review_step < maxStep 应当算进行中");
        assertEquals(1L, summary.getDueForReview(), "next_review_at <= now 应当算到期");

        assertEquals(List.of(otherWordId), vocabularyStudy.newWordIds(theme.getId(), accountId, 10),
                "newWordIds 只应返回还没有记忆行的词");
        assertEquals(List.of(otherWordId), vocabularyStudy.newWordIds(theme.getId(), accountId, 1),
                "LIMIT 没生效");
        assertEquals(0, vocabularyStudy.newWordIds(probeId(), accountId, 10).size());

        VocabularyMemoryLock lock = vocabularyStudy.lockMemory(accountId, wordId);
        assertNotNull(lock, "FOR UPDATE 读不到刚加入计划的词");
        assertEquals(0, lock.getMemoryCount(), "别名 memoryCount 丢失");
        assertEquals(0, lock.getReviewCount(), "别名 reviewCount 丢失");
        assertEquals(now, lock.getNextReviewAt(), "别名 nextReviewAt 丢失");

        VocabularyReviewSchedule schedule = VocabularyReviewSchedule.builder()
                .reviewNumber(1)
                .reviewStep(1)
                .intervalSeconds(3600L)
                .scheduledAt(now)
                .reviewedAt(now.plusMinutes(5))
                .nextReviewAt(now.plusHours(1))
                .timingStatus("ON_TIME")
                .build();
        assertEquals(1, vocabularyStudy.advanceMemory(accountId, wordId, schedule));
        VocabularyMemoryRow advanced = vocabularyStudy.memory(accountId, wordId);
        assertEquals(1, advanced.getMemoryCount(), "memory_count 应当在原值上累加");
        assertEquals(1, advanced.getReviewStep(), "review_step 没有写进去");
        assertEquals(1, advanced.getReviewCount(), "review_count 没有写进去");
        assertEquals(now, advanced.getFirstLearnedAt(), "已有的首次学习时间不能被覆盖");
        assertEquals(schedule.getReviewedAt(), advanced.getLastReviewedAt());
        assertEquals(schedule.getNextReviewAt(), advanced.getNextReviewAt());
        assertEquals("ACTIVE", advanced.getLearningStatus());
        assertEquals(1, vocabularyStudy.advanceMemory(accountId, wordId, schedule));
        assertEquals(2, vocabularyStudy.memory(accountId, wordId).getMemoryCount(),
                "memory_count 必须每次 +1");
        assertEquals(0, vocabularyStudy.advanceMemory(accountId, probeId(), schedule));
        assertNotNull(vocabularyStudy.memorySnapshot(accountId).get(0).getLastMemoryAt(),
                "advanceMemory 写了 last_memory_at 却读不到（检查别名 lastMemoryAt）");

        assertEquals(1, vocabularyStudy.upsertDisplay(accountId, wordId, "CHINESE_ONLY"));
        assertEquals("CHINESE_ONLY", vocabularyStudy.memory(accountId, wordId).getDisplayMode(),
                "memory() 的左连接应当把单卡显示偏好带出来");
        assertEquals("CHINESE_ONLY", vocabularyStudy.memories(accountId, List.of(wordId)).get(0).getDisplayMode(),
                "memories() 的左连接应当把单卡显示偏好带出来");

        assertEquals(1, vocabularyStudy.resetMemory(accountId, wordId));
        assertEquals(0, vocabularyStudy.resetMemory(accountId, wordId));
        assertNull(vocabularyStudy.memory(accountId, wordId));
        assertEquals(0L, vocabularyStudy.totalMemoryCount(accountId));
        assertEquals(0L, vocabularyStudy.activeCount(accountId));
        assertTrue(vocabularyStudy.memorySnapshot(accountId).isEmpty());
        assertEquals(0L, vocabularyStudy.dueCount(accountId, now));

        assertEquals(1, vocabularyStudy.initializeMemory(accountId, otherWordId, now));
        VocabularyMemoryRow initialized = vocabularyStudy.memory(accountId, otherWordId);
        assertNotNull(initialized);
        assertEquals(now, initialized.getFirstLearnedAt(), "initializeMemory 必须写入首次学习时间");
        assertNull(initialized.getNextReviewAt());
        assertEquals("ACTIVE", initialized.getLearningStatus());
        assertEquals(0, vocabularyStudy.introducedSince(accountId, now.plusMinutes(1)));
        assertEquals(0L, vocabularyStudy.dueCount(accountId, now),
                "next_review_at 为 null 时不应算到期");

        // importMemory 是「只增不减」：用更小的本机进度导入不能把账号上的进度改小
        // 单独用第三个词，让第一次 import 走 INSERT（返回 1）、第二次走 UPDATE（返回 2）
        VocabularyDto.Word importedWord = insertProbeWord(theme.getId());
        long importedWordId = importedWord.getId();
        LocalDateTime earlier = now.minusDays(10);
        assertEquals(1, vocabularyStudy.importMemory(accountId, importedWordId, 5, 3, 2, earlier,
                earlier, now.plusDays(3), earlier, now));
        VocabularyMemoryRow imported = vocabularyStudy.memory(accountId, importedWordId);
        assertEquals(5, imported.getMemoryCount(), "memory_count 没有被导入");
        assertEquals(3, imported.getReviewStep());
        assertEquals(2, imported.getReviewCount());
        assertEquals(earlier, imported.getFirstLearnedAt(), "更早的首次学习时间应当被采纳");
        assertEquals(now.plusDays(3), imported.getNextReviewAt());
        assertEquals(earlier, imported.getLastReviewedAt());

        assertEquals(2, vocabularyStudy.importMemory(accountId, importedWordId, 1, 1, 1, earlier,
                now, now.plusDays(5), now, now),
                "只有 last_reviewed_at / last_memory_at 被推进时也算改动了行");
        VocabularyMemoryRow notShrunk = vocabularyStudy.memory(accountId, importedWordId);
        assertEquals(5, notShrunk.getMemoryCount(), "memory_count 不能被更小的本机进度改小");
        assertEquals(3, notShrunk.getReviewStep(), "review_step 不能被更小的本机进度改小");
        assertEquals(2, notShrunk.getReviewCount(), "review_count 不能被更小的本机进度改小");
        assertEquals(now.plusDays(3), notShrunk.getNextReviewAt(),
                "已有的下次复习时间不能被覆盖（COALESCE）");
        assertEquals(earlier, notShrunk.getFirstLearnedAt(), "首次学习时间不能变晚");
        assertEquals(now, notShrunk.getLastReviewedAt(), "更晚的最近复习时间应当被采纳");
        assertEquals(5L, vocabularyStudy.totalMemoryCount(accountId),
                "两行记忆的 memory_count 之和应当是 0 + 5");
    }

    @Test
    void vocabularyReviewLogsAreIdempotentPerReviewSession() {
        long accountId = probeId();
        VocabularyDto.Word word = insertProbeWord(insertProbeTheme().getId());
        long wordId = word.getId();
        LocalDateTime now = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);

        String sessionId = UUID.randomUUID().toString();
        assertNull(vocabularyStudy.findReviewBySession(accountId, sessionId),
                "还没有写入时应当返回 null");

        VocabularyReviewSchedule first = VocabularyReviewSchedule.builder()
                .reviewNumber(1).reviewStep(1).intervalSeconds(3600L)
                .scheduledAt(now.minusHours(2)).reviewedAt(now)
                .nextReviewAt(now.plusHours(1)).timingStatus("ON_TIME").build();
        assertEquals(1, vocabularyStudy.insertReview(accountId, wordId, sessionId, "EN_TO_ZH", first));

        VocabularyReviewLogRow row = vocabularyStudy.findReviewBySession(accountId, sessionId);
        assertNotNull(row, "findReviewBySession 读不到刚写入的日志");
        assertEquals(wordId, row.getWordId(), "别名 wordId 丢失");
        assertEquals(1, row.getReviewNumber(), "别名 reviewNumber 丢失（XML 映射的是 review_number）");
        assertEquals(3600L, row.getIntervalSeconds(), "别名 intervalSeconds 丢失");
        assertEquals("ON_TIME", row.getTimingStatus(), "别名 timingStatus 丢失（XML 映射的是 timing_status）");

        assertEquals(0, vocabularyStudy.insertReview(accountId, wordId, sessionId, "EN_TO_ZH", first),
                "同一 review_session_id 重放必须被唯一键挡掉（INSERT IGNORE 返回 0）");
        assertEquals(1L, vocabularyStudy.countReviews(accountId), "重放不应重复计数");
        assertEquals(0L, vocabularyStudy.countReviews(probeId()), "别的账户的日志不应被算进来");

        LocalDateTime later = now.plusMinutes(10);
        String secondSession = UUID.randomUUID().toString();
        VocabularyReviewSchedule second = VocabularyReviewSchedule.builder()
                .reviewNumber(2).reviewStep(2).intervalSeconds(21600L)
                .scheduledAt(now).reviewedAt(later)
                .nextReviewAt(later.plusHours(6)).timingStatus("EARLY").build();
        assertEquals(1, vocabularyStudy.insertReview(accountId, wordId, secondSession, "ZH_TO_EN", second));
        assertEquals(2L, vocabularyStudy.countReviews(accountId));
        assertEquals(1L, vocabularyStudy.countReviewsSince(accountId, later),
                "只应统计窗口之后的日志");
        assertEquals(2L, vocabularyStudy.countReviewsSince(accountId, now.minusMinutes(1)));

        String importedSession = UUID.randomUUID().toString();
        assertEquals(1, vocabularyStudy.importReview(accountId, wordId, importedSession, "ZH_TO_EN", 3,
                now.minusHours(1), now, 7200L, "OVERDUE"));
        assertEquals(0, vocabularyStudy.importReview(accountId, wordId, importedSession, "ZH_TO_EN", 3,
                now.minusHours(1), now, 7200L, "OVERDUE"),
                "importReview 对同一 review_session_id 同样幂等");
        assertEquals(3L, vocabularyStudy.countReviews(accountId));

        List<VocabularyReviewHistoryRow> recent = vocabularyStudy.recentReviews(accountId, 10);
        assertEquals(3, recent.size());
        assertEquals(2, recent.get(0).getReviewNumber(), "历史必须按 reviewed_at DESC, id DESC");
        assertEquals(wordId, recent.get(0).getWordId(), "别名 wordId 丢失（XML 用的是 l.word_id）");
        assertEquals(word.getWord(), recent.get(0).getWord(),
                "别名 word 丢失（XML JOIN 了 sr_english_vocabulary_word）");
        assertEquals("ZH_TO_EN", recent.get(0).getDirection(), "别名 direction 丢失");
        assertEquals(21600L, recent.get(0).getIntervalSeconds(), "别名 intervalSeconds 丢失");
        assertEquals("EARLY", recent.get(0).getTimingStatus(), "别名 timingStatus 丢失");
        assertNotNull(recent.get(0).getScheduledAt(), "别名 scheduledAt 丢失");
        assertNotNull(recent.get(0).getReviewedAt(), "别名 reviewedAt 丢失");
        assertNotNull(recent.get(0).getId(), "别名 id 丢失");
        assertEquals(3, recent.get(1).getReviewNumber());
        assertEquals(7200L, recent.get(1).getIntervalSeconds());
        assertEquals("OVERDUE", recent.get(1).getTimingStatus());
        assertEquals(1, recent.get(2).getReviewNumber());
        assertEquals(3600L, recent.get(2).getIntervalSeconds());
        assertEquals(1, vocabularyStudy.recentReviews(accountId, 1).size(), "LIMIT 没生效");
        assertTrue(vocabularyStudy.recentReviews(probeId(), 10).isEmpty(),
                "别的账户的历史不应被读到");
    }

    @Test
    void writingPromptCrudHonoursPublicAndSearchFilters() {
        String search = unique("mapperxmlwritingprompt");
        WritingPromptDto.Prompt prompt = newPrompt(search);
        assertEquals(1, writingPrompts.insert(prompt));
        assertNotNull(prompt.getId(), "insert 没有回填自增主键");

        WritingPromptDto.Prompt stored = writingPrompts.byId(prompt.getId());
        assertNotNull(stored, "byId 读不到刚插入的行");
        assertEquals(prompt.getSlug(), stored.getSlug());
        assertEquals(search, stored.getTitle());
        assertEquals("XML 验证作文摘要", stored.getSummary());
        assertEquals("# XML 验证作文背景", stored.getBodyMarkdown(),
                "别名 bodyMarkdown 丢失（XML 映射的是 background_markdown）");
        assertEquals("## XML 验证作文要求", stored.getRequirementsMarkdown(),
                "别名 requirementsMarkdown 丢失（XML 映射的是 requirements_markdown）");
        assertEquals("B2", stored.getCefrLevel());
        assertEquals(180, stored.getWordMin().intValue(), "别名 wordMin 丢失（XML 映射的是 word_min）");
        assertEquals(260, stored.getWordMax().intValue(), "别名 wordMax 丢失");
        assertEquals(40, stored.getEstimatedMinutes().intValue(), "别名 estimatedMinutes 丢失");
        assertEquals("DRAFT", stored.getPublishStatus(),
                "insert 不写 publish_status，应当取列默认值 DRAFT");
        assertEquals(0, stored.getSortOrder());

        assertNull(writingPrompts.byId(-1L));
        assertNull(writingPrompts.publicBySlug(prompt.getSlug()), "草稿作文题不应能按 slug 公开读取");
        assertEquals(1, writingPrompts.count(false, search), "search 过滤没生效");
        assertEquals(0, writingPrompts.count(true, search), "publicOnly 过滤没生效");
        assertEquals(0, writingPrompts.count(false, search + "-missing"));
        assertTrue(writingPrompts.count(false, null) >= 1, "search 为 null 时必须退化成统计全部");
        assertEquals(1, writingPrompts.list(false, search, 0, 10).size());
        assertEquals(1, writingPrompts.list(false, search, 0, 1).size(), "LIMIT 没生效");
        assertTrue(writingPrompts.list(false, search, 1, 10).isEmpty(), "OFFSET 没生效");

        assertEquals(1, writingPrompts.setStatus(prompt.getId(), "PUBLISHED"));
        assertEquals("PUBLISHED", writingPrompts.byId(prompt.getId()).getPublishStatus());
        assertNotNull(writingPrompts.publicBySlug(prompt.getSlug()),
                "已发布作文题应当能按 slug 公开读取");
        assertEquals(1, writingPrompts.count(true, search));
        Object firstPublishedAt = publishedAtOf("sr_english_writing_prompt", prompt.getId());
        assertNotNull(firstPublishedAt, "setStatus 必须写入 published_at");
        assertEquals(1, writingPrompts.setStatus(prompt.getId(), "DRAFT"));
        assertEquals(1, writingPrompts.setStatus(prompt.getId(), "PUBLISHED"));
        assertEquals(firstPublishedAt, publishedAtOf("sr_english_writing_prompt", prompt.getId()),
                "重新发布把首次发布时间改写了（COALESCE 丢失）");

        WritingPromptDto.Prompt edited = writingPrompts.byId(prompt.getId());
        edited.setTitle(search + "-改名");
        edited.setWordMin(null);
        edited.setWordMax(null);
        edited.setEstimatedMinutes(null);
        assertEquals(1, writingPrompts.update(edited));
        WritingPromptDto.Prompt afterUpdate = writingPrompts.byId(prompt.getId());
        assertEquals(search + "-改名", afterUpdate.getTitle());
        assertNull(afterUpdate.getWordMin(), "word_min 传 null 没有被清空（这条语句是普通 SET）");
        assertNull(afterUpdate.getWordMax());
        assertNull(afterUpdate.getEstimatedMinutes());
        assertEquals("PUBLISHED", afterUpdate.getPublishStatus(), "改内容不应改动 publish_status");
        assertEquals("## XML 验证作文要求", afterUpdate.getRequirementsMarkdown());

        assertEquals(1, writingPrompts.delete(prompt.getId()));
        assertNull(writingPrompts.byId(prompt.getId()));
        assertEquals(0, writingPrompts.delete(prompt.getId()));
    }

    @Test
    void writingResourceCrudHonoursPublicAndSearchFilters() {
        String search = unique("mapperxmlwritingresource");
        WritingResourceDto.Resource resource = newResource(search);
        assertEquals(1, writingResources.insert(resource));
        assertNotNull(resource.getId(), "insert 没有回填自增主键");

        WritingResourceDto.Resource stored = writingResources.byId(resource.getId());
        assertNotNull(stored, "byId 读不到刚插入的行");
        assertEquals(resource.getSlug(), stored.getSlug());
        assertEquals("TEMPLATE", stored.getResourceKind(),
                "别名 resourceKind 丢失（XML 映射的是 resource_kind）");
        assertEquals(search, stored.getTitle());
        assertEquals("XML 验证资源摘要", stored.getSummary());
        assertEquals("# XML 验证资源正文", stored.getBodyMarkdown(), "别名 bodyMarkdown 丢失");
        assertEquals("B1", stored.getCefrLevel());
        assertEquals("DRAFT", stored.getPublishStatus(),
                "insert 不写 publish_status，应当取列默认值 DRAFT");
        assertEquals(0, stored.getSortOrder());

        assertNull(writingResources.byId(-1L));
        assertNull(writingResources.publicBySlug(resource.getSlug()), "草稿资源不应能按 slug 公开读取");
        assertEquals(1, writingResources.count(false, search), "search 过滤没生效");
        assertEquals(0, writingResources.count(true, search), "publicOnly 过滤没生效");
        assertEquals(0, writingResources.count(false, search + "-missing"));
        assertTrue(writingResources.count(false, null) >= 1, "search 为 null 时必须退化成统计全部");
        assertEquals(1, writingResources.list(false, search, 0, 10).size());
        assertEquals(1, writingResources.list(false, search, 0, 1).size(), "LIMIT 没生效");
        assertTrue(writingResources.list(false, search, 1, 10).isEmpty(), "OFFSET 没生效");

        assertEquals(1, writingResources.setStatus(resource.getId(), "PUBLISHED"));
        assertEquals("PUBLISHED", writingResources.byId(resource.getId()).getPublishStatus());
        assertNotNull(writingResources.publicBySlug(resource.getSlug()),
                "已发布资源应当能按 slug 公开读取");
        assertEquals(1, writingResources.count(true, search));
        Object firstPublishedAt = publishedAtOf("sr_english_writing_resource", resource.getId());
        assertNotNull(firstPublishedAt, "setStatus 必须写入 published_at");
        assertEquals(1, writingResources.setStatus(resource.getId(), "DRAFT"));
        assertEquals(1, writingResources.setStatus(resource.getId(), "PUBLISHED"));
        assertEquals(firstPublishedAt, publishedAtOf("sr_english_writing_resource", resource.getId()),
                "重新发布把首次发布时间改写了（COALESCE 丢失）");

        // 这张表除主键外没有可空列（summary/body_markdown 都是 NOT NULL），只验证改动的列生效
        WritingResourceDto.Resource edited = writingResources.byId(resource.getId());
        edited.setTitle(search + "-改名");
        edited.setResourceKind("CHECKLIST");
        edited.setSlug(resource.getSlug() + "-renamed");
        assertEquals(1, writingResources.update(edited));
        WritingResourceDto.Resource afterUpdate = writingResources.byId(resource.getId());
        assertEquals(search + "-改名", afterUpdate.getTitle());
        assertEquals("CHECKLIST", afterUpdate.getResourceKind());
        assertEquals(resource.getSlug() + "-renamed", afterUpdate.getSlug());
        assertEquals("# XML 验证资源正文", afterUpdate.getBodyMarkdown(), "改内容不应动正文");
        assertEquals("PUBLISHED", afterUpdate.getPublishStatus(), "改内容不应改动 publish_status");

        assertEquals(1, writingResources.delete(resource.getId()));
        assertNull(writingResources.byId(resource.getId()));
        assertEquals(0, writingResources.delete(resource.getId()));
    }

    @Test
    void rollbackLeavesNoEnglishProbeRowsAndRestoresTheGrammarCourse() {
        GrammarDto.Course before = grammar.course();
        assertNotNull(before);
        String originalTitle = before.getTitle();
        String originalStatus = before.getPublishStatus();

        GrammarDto.Course edited = grammar.course();
        edited.setTitle("XML 回滚标题");
        assertEquals(1, grammar.updateCourse(edited));

        GrammarDto.Section section = new GrammarDto.Section();
        section.setTitle("XML 回滚章节");
        section.setSortOrder(1);
        grammar.insertSection(section);
        GrammarDto.Lesson lesson = newLesson(section.getId(), unique("mapperxmlrollbacklesson"));
        grammar.insertLesson(lesson);

        ListeningItemDto.Item item = newItem(unique("mapperxmlrollbacklistening"));
        listeningItems.insert(item);
        ListeningSegmentDto.Segment segment = newSegment(item.getId(), 1, 0, "回滚切片");
        listeningSegments.insert(segment);

        ReadingDto.Article article = newArticle(unique("mapperxmlrollbackreading"));
        readings.insert(article);

        VocabularyDto.Theme theme = insertProbeTheme();
        VocabularyDto.Word word = insertProbeWord(theme.getId());
        long accountId = probeId();
        vocabularyStudy.startMemory(accountId, word.getId(), now());
        vocabularyStudy.insertReview(accountId, word.getId(), UUID.randomUUID().toString(),
                "EN_TO_ZH", VocabularyReviewSchedule.builder()
                        .reviewNumber(1).reviewStep(1).intervalSeconds(60L)
                        .reviewedAt(now()).nextReviewAt(now()).timingStatus("NEW").build());

        WritingPromptDto.Prompt prompt = newPrompt(unique("mapperxmlrollbackprompt"));
        writingPrompts.insert(prompt);
        WritingResourceDto.Resource resource = newResource(unique("mapperxmlrollbackresource"));
        writingResources.insert(resource);

        Long sectionId = section.getId();
        Long lessonId = lesson.getId();
        Long itemId = item.getId();
        Long articleId = article.getId();
        Long themeId = theme.getId();
        Long wordId = word.getId();
        Long promptId = prompt.getId();
        Long resourceId = resource.getId();
        assertNotNull(sectionId);
        assertNotNull(itemId);
        assertNotNull(articleId);
        assertNotNull(themeId);
        assertNotNull(wordId);

        session.rollback();
        try (SqlSession fresh = openSession()) {
            GrammarMapper freshGrammar = fresh.getMapper(GrammarMapper.class);
            assertEquals(originalTitle, freshGrammar.course().getTitle(),
                    "回滚后语法课程标题应当还原");
            assertEquals(originalStatus, freshGrammar.course().getPublishStatus(),
                    "回滚后语法课程发布状态应当还原");
            assertNull(freshGrammar.section(sectionId), "回滚后不应在开发库里留下测试语法章节");
            assertNull(freshGrammar.lessonById(lessonId), "回滚后不应在开发库里留下测试语法课时");

            ListeningItemMapper freshListening = fresh.getMapper(ListeningItemMapper.class);
            assertNull(freshListening.byId(itemId), "回滚后不应在开发库里留下测试听力");
            assertTrue(fresh.getMapper(ListeningSegmentMapper.class).listForAdmin(itemId).isEmpty(),
                    "回滚后不应在开发库里留下测试听力切片");

            assertNull(fresh.getMapper(ReadingMapper.class).byId(articleId),
                    "回滚后不应在开发库里留下测试阅读文章");

            VocabularyMapper freshVocabulary = fresh.getMapper(VocabularyMapper.class);
            assertNull(freshVocabulary.word(wordId), "回滚后不应在开发库里留下测试单词");
            assertNull(freshVocabulary.theme(themeId), "回滚后不应在开发库里留下测试主题");
            VocabularyStudyMapper freshStudy = fresh.getMapper(VocabularyStudyMapper.class);
            assertNull(freshStudy.memory(accountId, wordId), "回滚后不应在开发库里留下测试记忆行");
            assertEquals(0L, freshStudy.countReviews(accountId), "回滚后不应在开发库里留下测试复习日志");

            assertNull(fresh.getMapper(WritingPromptMapper.class).byId(promptId),
                    "回滚后不应在开发库里留下测试作文题");
            assertNull(fresh.getMapper(WritingResourceMapper.class).byId(resourceId),
                    "回滚后不应在开发库里留下测试写作资源");
        }
    }

    /* 六条 setStatus 的 published_at 不在任何 resultMap 里，只能直接读库核对 COALESCE 语义 */
    private Object publishedAtOf(String table, long id) {
        try (PreparedStatement statement = session.getConnection().prepareStatement(
                "SELECT published_at FROM " + table + " WHERE id = ?")) {
            statement.setLong(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                assertTrue(rows.next(), table + " 里找不到 id = " + id + " 的行");
                return rows.getObject(1);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("读取 " + table + ".published_at 失败", exception);
        }
    }

    /*
     * sr_english_vocabulary_word_audio 的数据来自 V1 导入，Mapper 里没有写入语句，
     * 所以这里在同一个未提交事务里造一条授权发音，专门验证 audios() 的 resultMap。
     */
    private long insertAudioFixture(long wordId, long mediaAssetId) {
        try (PreparedStatement statement = session.getConnection().prepareStatement(
                "INSERT INTO sr_english_vocabulary_word_audio"
                        + " (word_id, accent, media_asset_id, provider, source_url, license_note, is_primary)"
                        + " VALUES (?, 'US', ?, 'UPLOADED', 'https://example.test/audio.mp3',"
                        + " 'XML 验证授权说明', 1)",
                PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, wordId);
            statement.setLong(2, mediaAssetId);
            assertEquals(1, statement.executeUpdate());
            try (ResultSet keys = statement.getGeneratedKeys()) {
                assertTrue(keys.next(), "造授权发音没有拿到自增主键");
                return keys.getLong(1);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("造授权发音失败", exception);
        }
    }

    private GrammarDto.Lesson newLesson(long sectionId, String slug) {
        GrammarDto.Lesson lesson = new GrammarDto.Lesson();
        lesson.setSectionId(sectionId);
        lesson.setTitle("XML 验证课时");
        lesson.setSlug(slug);
        lesson.setSummary("XML 验证课时摘要");
        lesson.setBodyMarkdown("# XML 验证课时正文");
        lesson.setSortOrder(0);
        return lesson;
    }

    private ListeningItemDto.Item newItem(String title) {
        ListeningItemDto.Item item = new ListeningItemDto.Item();
        item.setSlug(unique("mapperxmllisteningslug"));
        item.setTitle(title);
        item.setSummary("XML 验证听力摘要");
        item.setBodyMarkdown("# XML 验证听力原文");
        item.setCefrLevel("B1");
        item.setDifficultyLevel(3);
        item.setAudioMediaId(probeId());
        item.setDurationSeconds(300);
        item.setSourceName("XML 验证来源");
        item.setSourceUrl("https://example.test/listening");
        item.setSortOrder(0);
        return item;
    }

    private ListeningItemDto.Item insertPublishedItem() {
        ListeningItemDto.Item item = newItem(unique("mapperxmllistening"));
        listeningItems.insert(item);
        assertEquals(1, listeningItems.setStatus(item.getId(), "PUBLISHED"));
        return item;
    }

    private ListeningSegmentDto.Segment newSegment(long itemId, int sortOrder, int startMs, String text) {
        ListeningSegmentDto.Segment segment = new ListeningSegmentDto.Segment();
        segment.setListeningItemId(itemId);
        segment.setStartMs(startMs);
        segment.setEndMs(startMs + 500);
        segment.setTranscriptText(text);
        segment.setTranslationText("XML 验证翻译");
        segment.setSortOrder(sortOrder);
        return segment;
    }

    private ListeningSegmentDto.Segment withSegmentItemId(ListeningSegmentDto.Segment source, long itemId) {
        ListeningSegmentDto.Segment copy = new ListeningSegmentDto.Segment();
        copy.setId(source.getId());
        copy.setListeningItemId(itemId);
        copy.setStartMs(source.getStartMs());
        copy.setEndMs(source.getEndMs());
        copy.setTranscriptText(source.getTranscriptText());
        copy.setTranslationText(source.getTranslationText());
        copy.setSortOrder(source.getSortOrder());
        return copy;
    }

    private ReadingDto.Article newArticle(String title) {
        ReadingDto.Article article = new ReadingDto.Article();
        article.setSlug(unique("mapperxmlreadingslug"));
        article.setTitle(title);
        article.setSummary("XML 验证阅读摘要");
        article.setBodyMarkdown("# XML 验证阅读正文");
        article.setCefrLevel("A2");
        article.setDifficultyLevel(4);
        article.setSourceName("XML 验证来源");
        article.setSourceUrl("https://example.test/reading");
        article.setSortOrder(0);
        return article;
    }

    private VocabularyDto.Theme newTheme() {
        VocabularyDto.Theme theme = new VocabularyDto.Theme();
        theme.setLayer("XML 验证层级");
        theme.setLayerOrder(7);
        theme.setName(unique("mapperxmlvocabtheme"));
        theme.setSortOrder(3);
        return theme;
    }

    private VocabularyDto.Word newWord(long themeId, String text) {
        VocabularyDto.Word word = new VocabularyDto.Word();
        word.setThemeId(themeId);
        word.setWord(text);
        word.setPartOfSpeech("n.");
        word.setPhoneticUs("/ˈtest/");
        word.setPhoneticUk("/ˈtest-uk/");
        word.setTranslation("XML 验证释义");
        word.setSceneMeaning("XML 验证本主题用法");
        word.setInflections("mapperxml-inflection");
        word.setExamples("[{\"en\":\"XML verify\"}]");
        word.setSortOrder(5);
        return word;
    }

    private VocabularyDto.Word withWordId(Long id, VocabularyDto.Word source) {
        VocabularyDto.Word copy = new VocabularyDto.Word();
        copy.setId(id);
        copy.setThemeId(source.getThemeId());
        copy.setWord(source.getWord());
        copy.setPartOfSpeech(source.getPartOfSpeech());
        copy.setPhoneticUs(source.getPhoneticUs());
        copy.setPhoneticUk(source.getPhoneticUk());
        copy.setTranslation(source.getTranslation());
        copy.setSceneMeaning(source.getSceneMeaning());
        copy.setInflections(source.getInflections());
        copy.setExamples(source.getExamples());
        copy.setSortOrder(source.getSortOrder());
        return copy;
    }

    private VocabularyDto.Theme insertProbeTheme() {
        VocabularyDto.Theme theme = newTheme();
        assertEquals(1, vocabulary.insertTheme(theme));
        return theme;
    }

    private VocabularyDto.Word insertProbeWord(long themeId) {
        VocabularyDto.Word word = newWord(themeId, unique("mapperxmlword"));
        assertEquals(1, vocabulary.insertWord(word));
        return word;
    }

    private WritingPromptDto.Prompt newPrompt(String title) {
        WritingPromptDto.Prompt prompt = new WritingPromptDto.Prompt();
        prompt.setSlug(unique("mapperxmlwritingpromptslug"));
        prompt.setTitle(title);
        prompt.setSummary("XML 验证作文摘要");
        prompt.setBodyMarkdown("# XML 验证作文背景");
        prompt.setRequirementsMarkdown("## XML 验证作文要求");
        prompt.setCefrLevel("B2");
        prompt.setWordMin(180);
        prompt.setWordMax(260);
        prompt.setEstimatedMinutes(40);
        prompt.setSortOrder(0);
        return prompt;
    }

    private WritingResourceDto.Resource newResource(String title) {
        WritingResourceDto.Resource resource = new WritingResourceDto.Resource();
        resource.setResourceKind("TEMPLATE");
        resource.setTitle(title);
        resource.setSlug(unique("mapperxmlwritingresourceslug"));
        resource.setSummary("XML 验证资源摘要");
        resource.setBodyMarkdown("# XML 验证资源正文");
        resource.setCefrLevel("B1");
        resource.setSortOrder(0);
        return resource;
    }

    private ListeningItemDto.Item withItemId(Long id, ListeningItemDto.Item source) {
        ListeningItemDto.Item copy = new ListeningItemDto.Item();
        copy.setId(id);
        copy.setSlug(source.getSlug());
        copy.setTitle(source.getTitle());
        copy.setSummary(source.getSummary());
        copy.setBodyMarkdown(source.getBodyMarkdown());
        copy.setCefrLevel(source.getCefrLevel());
        copy.setDifficultyLevel(source.getDifficultyLevel());
        copy.setAudioMediaId(source.getAudioMediaId());
        copy.setDurationSeconds(source.getDurationSeconds());
        copy.setSourceName(source.getSourceName());
        copy.setSourceUrl(source.getSourceUrl());
        copy.setPublishStatus(source.getPublishStatus());
        copy.setSortOrder(source.getSortOrder());
        return copy;
    }

    /* 逻辑外键，库里没有物理 FOREIGN KEY：用一个大号段避免和真实内容撞号 */
    private long probeId() {
        return 900_000_000_000L + PROBE.incrementAndGet();
    }

    private String unique(String prefix) {
        return prefix + "-" + PROBE.incrementAndGet() + "-" + System.nanoTime();
    }

    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
    }
}
