/*
 * TutorialPublishedDocument：教程与学习模块的跨模块接口文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */

package com.starrainnotes.tutorial.content.api;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 教程已发布文档，属于教程与学习模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorialPublishedDocument {
    /**
     * 当前业务对象的数据库标识。
     */
    private Long id;
    /**
     * 公开固定链接的路径标识。
     */
    private String slug;
    /**
     * 标题。
     */
    private String title;
    /**
     * 摘要。
     */
    private String summary;
    /**
     * 已发布时间。
     */
    private LocalDateTime publishedAt;
    /**
     * 章节列表。
     */
    private List<TutorialChapterDocument> chapters;
    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;

    /**
     * 构造TutorialPublishedDocument，接收本类使用的依赖或初始化参数。
     * 关键处理步骤调用 <code>this</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @param title 标题；类型为 <code>String</code>
     * @param summary 摘要；类型为 <code>String</code>
     * @param publishedAt 已发布时间；类型为 <code>LocalDateTime</code>
     * @param chapters 章节列表；类型为 <code>List&lt;TutorialChapterDocument&gt;</code>
     */
    public TutorialPublishedDocument(Long id, String slug, String title, String summary, LocalDateTime publishedAt, List<TutorialChapterDocument> chapters) {
        this(id, slug, title, summary, publishedAt, chapters, null);
    }
}
