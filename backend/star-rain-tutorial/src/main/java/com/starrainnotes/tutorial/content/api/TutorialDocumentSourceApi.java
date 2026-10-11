/*
 * TutorialDocumentSourceApi：教程与学习模块的跨模块接口文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 * 主要协作类型：PageResult。
 * 阅读入口：page（分页读取教程文档来源）；bySlug（依据固定链接标识处理入口）；chapter（章节处理入口）。
 */

package com.starrainnotes.tutorial.content.api;

import com.starrainnotes.common.result.PageResult;
import java.util.Optional;

/** Public documents for consumers such as Search and SEO; never exposes drafts. */
public interface TutorialDocumentSourceApi {
    /**
     * 分页读取教程文档来源。
     *
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @return 类型为 <code>PageResult&lt;TutorialPublishedDocument&gt;</code> 的处理结果
     */
    PageResult<TutorialPublishedDocument> page(int page, int pageSize);
    /**
     * 依据固定链接标识处理入口。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @return 可选结果；没有匹配值时由空 Optional 表达
     */
    Optional<TutorialPublishedDocument> bySlug(String slug);
    /**
     * 章节处理入口。
     *
     * @param tutorialSlug 教程固定链接标识；类型为 <code>String</code>
     * @param chapterSlug 章节固定链接标识；类型为 <code>String</code>
     * @return 可选结果；没有匹配值时由空 Optional 表达
     */
    Optional<TutorialChapterDocument> chapter(String tutorialSlug, String chapterSlug);
}
