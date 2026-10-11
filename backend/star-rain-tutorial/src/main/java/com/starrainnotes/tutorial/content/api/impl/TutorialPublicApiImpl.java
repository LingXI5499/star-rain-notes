/*
 * TutorialPublicApiImpl：教程与学习模块的跨模块接口文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 * 主要协作类型：PublishedTutorial、TutorialPublicApi、TutorialPublicationService。
 * 阅读入口：latestPublished（最新已发布处理入口）。
 */

package com.starrainnotes.tutorial.content.api.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.starrainnotes.tutorial.content.api.PublishedTutorial;
import com.starrainnotes.tutorial.content.api.TutorialPublicApi;
import com.starrainnotes.tutorial.content.service.TutorialPublicationService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 教程公开接口实现，属于教程与学习模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
@Service
@RequiredArgsConstructor
public class TutorialPublicApiImpl implements TutorialPublicApi {
    /**
     * 发布。
     * 协作类型：<code>TutorialPublicationService</code>。
     */
    private final TutorialPublicationService publication;

    /**
     * 最新已发布处理入口。
     * 主要协作调用：<code>publication.publicTutorials</code>。
     *
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>
     * @return 本次处理得到的条目集合
     */
    @Override
    public List<PublishedTutorial> latestPublished(int limit) {
        int size = Math.max(1, Math.min(limit, 100));
        return publication.publicTutorials(null, null, 1, size).getItems().stream()
                .map(this::summary).toList();
    }

    /**
     * 摘要处理入口。
     * 关键处理步骤调用 <code>item.path</code>、<code>published.isBlank</code>、<code>LocalDateTime.parse</code>。
     *
     * @param item item；类型为 <code>JsonNode</code>
     * @return 类型为 <code>PublishedTutorial</code> 的处理结果
     */
    private PublishedTutorial summary(JsonNode item) {
        String published = item.path("publishedAt").asText("");
        return new PublishedTutorial(item.path("id").asLong(), item.path("slug").asText(),
                item.path("title").asText(), item.path("summary").asText(),
                item.path("coverUrl").asText(null),
                published.isBlank() ? null : LocalDateTime.parse(published));
    }
}
