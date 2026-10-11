/*
 * VocabularyService：英语学习模块的业务契约文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：Page、Theme、ThemeRequest、Word、WordRequest。
 * 阅读入口：themes（themes处理入口）；words（words处理入口）；word（单词处理入口）；wordsByIds（按标识集合读取words）。
 */

package com.starrainnotes.english.vocabulary.service;

import com.starrainnotes.english.vocabulary.dto.VocabularyDto.Page;
import com.starrainnotes.english.vocabulary.dto.VocabularyDto.Theme;
import com.starrainnotes.english.vocabulary.dto.VocabularyDto.ThemeRequest;
import com.starrainnotes.english.vocabulary.dto.VocabularyDto.Word;
import com.starrainnotes.english.vocabulary.dto.VocabularyDto.WordRequest;
import java.util.List;

// 词库主题与单词的读写入口。
/**
 * 词汇业务服务，属于英语学习模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface VocabularyService {

    /**
     * themes处理入口。
     * @return 本次处理得到的条目集合
     */
    List<Theme> themes();

    /**
     * words处理入口。
     *
     * @param themeId 主题标识；类型为 <code>String</code>
     * @param search 搜索；类型为 <code>String</code>
     * @param page 分页页码；类型为 <code>int</code>
     * @param size 大小；类型为 <code>int</code>
     * @return 类型为 <code>Page&lt;Word&gt;</code> 的处理结果
     */
    Page<Word> words(String themeId, String search, int page, int size);

    /**
     * 单词处理入口。
     *
     * @param wordId 单词标识；类型为 <code>String</code>
     * @return 类型为 <code>Word</code> 的处理结果
     */
    Word word(String wordId);

    /**
     * 按标识集合读取words。
     *
     * @param wordIds 单词标识集合；类型为 <code>List&lt;Long&gt;</code>
     * @param maxWords 最大Words；类型为 <code>int</code>
     * @return 本次处理得到的条目集合
     */
    List<Word> wordsByIds(List<Long> wordIds, int maxWords);

    /**
     * 创建主题。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>ThemeRequest</code>
     * @return 类型为 <code>Theme</code> 的处理结果
     */
    Theme createTheme(ThemeRequest request);

    /**
     * 更新主题。
     *
     * @param themeId 主题标识；类型为 <code>String</code>
     * @param request 本次操作的请求参数对象；类型为 <code>ThemeRequest</code>
     * @return 类型为 <code>Theme</code> 的处理结果
     */
    Theme updateTheme(String themeId, ThemeRequest request);

    /**
     * 删除主题。
     *
     * @param themeId 主题标识；类型为 <code>String</code>
     */
    void deleteTheme(String themeId);

    /**
     * 创建单词。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>WordRequest</code>
     * @return 类型为 <code>Word</code> 的处理结果
     */
    Word createWord(WordRequest request);

    /**
     * 更新单词。
     *
     * @param wordId 单词标识；类型为 <code>String</code>
     * @param request 本次操作的请求参数对象；类型为 <code>WordRequest</code>
     * @return 类型为 <code>Word</code> 的处理结果
     */
    Word updateWord(String wordId, WordRequest request);

    /**
     * 删除单词。
     *
     * @param wordId 单词标识；类型为 <code>String</code>
     */
    void deleteWord(String wordId);
}
