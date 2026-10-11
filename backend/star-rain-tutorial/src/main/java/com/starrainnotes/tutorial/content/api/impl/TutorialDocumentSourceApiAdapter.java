/*
 * TutorialDocumentSourceApiAdapter：教程与学习模块的跨模块接口文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 * 主要协作类型：PageResult、TutorialChapterDocument、TutorialDocumentSourceApi、TutorialPublishedDocument、TutorialNotFoundException、TutorialPublicationService。
 * 阅读入口：page（分页读取教程文档来源接口Adapter）；bySlug（依据固定链接标识处理入口）；chapter（章节处理入口）。
 */

package com.starrainnotes.tutorial.content.api.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.tutorial.content.api.TutorialChapterDocument;
import com.starrainnotes.tutorial.content.api.TutorialDocumentSourceApi;
import com.starrainnotes.tutorial.content.api.TutorialPublishedDocument;
import com.starrainnotes.tutorial.content.exception.TutorialNotFoundException;
import com.starrainnotes.tutorial.content.service.TutorialPublicationService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 教程文档来源接口Adapter，属于教程与学习模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
@Component
@RequiredArgsConstructor
public class TutorialDocumentSourceApiAdapter implements TutorialDocumentSourceApi {
    /**
     * 发布。
     * 协作类型：<code>TutorialPublicationService</code>。
     */
    private final TutorialPublicationService publication;

    /**
     * 分页读取教程文档来源接口Adapter。
     * 主要协作调用：<code>publication.publicTutorials</code>。
     *
     * @param page 分页页码；类型为 <code>int</code>
     * @param pageSize 单页返回条目数；类型为 <code>int</code>
     * @return 类型为 <code>PageResult&lt;TutorialPublishedDocument&gt;</code> 的处理结果
     */
    @Override
    public PageResult<TutorialPublishedDocument> page(int page, int pageSize) {
        PageResult<JsonNode> result = publication.publicTutorials(null, null, page, pageSize);
        return PageResult.<TutorialPublishedDocument>builder()
            .items(result.getItems().stream().map(row -> bySlug(row.path("slug").asText()).orElseThrow()).toList())
            .page(result.getPage()).pageSize(result.getPageSize()).total(result.getTotal()).build();
    }

    /**
     * 依据固定链接标识处理入口。
     * 主要协作调用：<code>publication.publicTutorial</code>。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @return 可选结果；没有匹配值时由空 Optional 表达
     */
    @Override
    public Optional<TutorialPublishedDocument> bySlug(String slug) {
        try {
            JsonNode detail = publication.publicTutorial(slug);
            List<TutorialChapterDocument> chapters = new ArrayList<>();
            for (JsonNode group : detail.path("groups")) {
                for (JsonNode item : group.path("chapters")) {
                    chapter(slug, item.path("slug").asText()).ifPresent(chapters::add);
                }
            }
            String published = detail.path("publishedAt").asText("");
            TutorialPublishedDocument document = new TutorialPublishedDocument(id(detail), detail.path("slug").asText(),
                detail.path("title").asText(), detail.path("summary").asText(),
                published.isBlank() ? null : LocalDateTime.parse(published), List.copyOf(chapters));
            LocalDateTime modified = modifiedAt(detail);
            for (var chapter : chapters) {
                if (chapter.getUpdatedAt() != null && (modified == null || chapter.getUpdatedAt().isAfter(modified))) modified = chapter.getUpdatedAt();
            }
            document.setUpdatedAt(modified);
            return Optional.of(document);
        } catch (TutorialNotFoundException absent) {
            return Optional.empty();
        }
    }

    /**
     * 章节处理入口。
     * 主要协作调用：<code>publication.publicChapter</code>。
     *
     * @param tutorialSlug 教程固定链接标识；类型为 <code>String</code>
     * @param chapterSlug 章节固定链接标识；类型为 <code>String</code>
     * @return 可选结果；没有匹配值时由空 Optional 表达
     */
    @Override
    public Optional<TutorialChapterDocument> chapter(String tutorialSlug, String chapterSlug) {
        try {
            JsonNode detail = publication.publicChapter(tutorialSlug, chapterSlug);
            StringBuilder searchable = new StringBuilder(detail.path("bodyMarkdown").asText());
            for (JsonNode card : detail.path("cards")) {
                searchable.append(' ').append(card.path("frontText").asText());
                searchable.append(' ').append(card.path("backMarkdown").asText());
            }
            for (JsonNode question : detail.path("questions")) {
                searchable.append(' ').append(question.path("questionText").asText());
            }
            TutorialChapterDocument document = new TutorialChapterDocument(id(detail), detail.path("slug").asText(),
                detail.path("title").asText(), detail.path("summary").asText(),
                detail.path("bodyMarkdown").asText(), searchable.toString());
            document.setUpdatedAt(modifiedAt(detail));
            return Optional.of(document);
        } catch (TutorialNotFoundException absent) {
            return Optional.empty();
        }
    }

    /**
     * 当前业务对象的数据库标识处理入口。
     * 关键处理步骤调用 <code>Long.valueOf</code>、<code>node.path</code>。
     *
     * @param node node；类型为 <code>JsonNode</code>
     * @return 类型为 <code>Long</code> 的处理结果
     */
    private Long id(JsonNode node) { return Long.valueOf(node.path("id").asText()); }

    /**
     * modified时间处理入口。
     * 关键处理步骤调用 <code>publishedSnapshot.path</code>、<code>value.isBlank</code>、<code>LocalDateTime.parse</code>。
     *
     * @param publishedSnapshot 已发布快照；类型为 <code>JsonNode</code>
     * @return 类型为 <code>LocalDateTime</code> 的处理结果
     */
    private LocalDateTime modifiedAt(JsonNode publishedSnapshot) {
        String value = publishedSnapshot.path("updatedAt").asText("");
        if (value.isBlank()) return null;
        try { return LocalDateTime.parse(value); }
        catch (java.time.format.DateTimeParseException absentDate) { return null; }
    }
}
