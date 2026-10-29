/*
 * VocabularyMapper：英语学习模块的数据访问文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：Theme、Word、VocabularyWordAudioVO。
 * 阅读入口：themes（themes处理入口）；theme（主题处理入口）；countWords（统计Words）；words（words处理入口）。
 */

package com.starrainnotes.english.vocabulary.mapper;

import com.starrainnotes.english.vocabulary.dto.VocabularyDto.Theme;
import com.starrainnotes.english.vocabulary.dto.VocabularyDto.Word;
import com.starrainnotes.english.vocabulary.vo.VocabularyWordAudioVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 词汇数据映射，属于英语学习模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
@Mapper
public interface VocabularyMapper {
    /** New words enter editorial classification without changing their learning identity. */
    int initializeLibraryMapping(Word word);
    int deleteLibraryTopics(@Param("id") long id);
    int deleteLibraryMapping(@Param("id") long id);
    /**
     * themes处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_vocabulary_theme</code>、<code>sr_english_vocabulary_word</code>。
     * @return 符合 SQL 条件的记录数量
     */
    List<Theme> themes();
    /**
     * 主题处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_vocabulary_theme</code>、<code>sr_english_vocabulary_word</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 符合 SQL 条件的记录数量
     */
    Theme theme(@Param("id") long id);
    /**
     * 统计Words。
     *
     * 持久化操作：读取，涉及 <code>sr_english_vocabulary_word</code>。
     *
     * @param themeId 主题标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("themeId")</code>
     * @param search 搜索；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("search")</code>
     * @return 符合 SQL 条件的记录数量
     */
    long countWords(@Param("themeId") Long themeId, @Param("search") String search);
    /**
     * words处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_vocabulary_word</code>。
     * SQL 使用 LIMIT 限制读取数量；固定上限或分页参数以映射语句为准。
     *
     * @param themeId 主题标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("themeId")</code>
     * @param search 搜索；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("search")</code>
     * @param offset 查询跳过的记录数；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("offset")</code>
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("limit")</code>
     * @return 本次处理得到的条目集合
     */
    List<Word> words(@Param("themeId") Long themeId, @Param("search") String search,
                     @Param("offset") long offset, @Param("limit") int limit);
    /**
     * 单词处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_vocabulary_word</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>Word</code> 的处理结果
     */
    Word word(@Param("id") long id);
    /**
     * 按标识集合读取words。
     *
     * 持久化操作：读取，涉及 <code>sr_english_vocabulary_word</code>。
     *
     * @param ids 标识集合；类型为 <code>List&lt;Long&gt;</code>；对应 MyBatis 命名参数 <code>@Param("ids")</code>
     * @return 本次处理得到的条目集合
     */
    List<Word> wordsByIds(@Param("ids") List<Long> ids);
    /**
     * audios处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_vocabulary_word_audio</code>。
     *
     * @param wordIds 单词标识集合；类型为 <code>List&lt;Long&gt;</code>；对应 MyBatis 命名参数 <code>@Param("wordIds")</code>
     * @return 本次处理得到的条目集合
     */
    List<VocabularyWordAudioVO> audios(@Param("wordIds") List<Long> wordIds);
    /**
     * 写入主题。
     *
     * 持久化操作：新增，涉及 <code>sr_english_vocabulary_theme</code>。
     *
     * @param theme 主题；类型为 <code>Theme</code>
     * @return SQL 写入影响的记录行数
     */
    int insertTheme(Theme theme);
    /**
     * 更新主题。
     *
     * 持久化操作：更新，涉及 <code>sr_english_vocabulary_theme</code>。
     *
     * @param theme 主题；类型为 <code>Theme</code>
     * @return SQL 写入影响的记录行数
     */
    int updateTheme(Theme theme);
    /**
     * 删除主题。
     *
     * 持久化操作：删除，涉及 <code>sr_english_vocabulary_theme</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return SQL 写入影响的记录行数
     */
    int deleteTheme(@Param("id") long id);
    /**
     * 写入单词。
     *
     * 持久化操作：新增，涉及 <code>sr_english_vocabulary_word</code>。
     *
     * @param word 单词；类型为 <code>Word</code>
     * @return SQL 写入影响的记录行数
     */
    int insertWord(Word word);
    /**
     * 更新单词。
     *
     * 持久化操作：更新，涉及 <code>sr_english_vocabulary_word</code>。
     *
     * @param word 单词；类型为 <code>Word</code>
     * @return SQL 写入影响的记录行数
     */
    int updateWord(Word word);
    /**
     * 删除单词。
     *
     * 持久化操作：删除，涉及 <code>sr_english_vocabulary_word</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return SQL 写入影响的记录行数
     */
    int deleteWord(@Param("id") long id);
}
