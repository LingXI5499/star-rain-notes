/*
 * TutorialReferenceApiImpl：教程与学习模块的跨模块接口文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 * 主要协作类型：PublishedTutorial、TutorialReferenceApi、TutorialEntity、TutorialMapper、TutorialPublicationService。
 * 阅读入口：exists（判断是否存在教程引用）；publishedTutorial（已发布教程处理入口）。
 */

package com.starrainnotes.tutorial.content.api.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.starrainnotes.tutorial.content.api.PublishedTutorial;
import com.starrainnotes.tutorial.content.api.TutorialReferenceApi;
import com.starrainnotes.tutorial.content.entity.TutorialEntity;
import com.starrainnotes.tutorial.content.mapper.TutorialMapper;
import com.starrainnotes.tutorial.content.service.TutorialPublicationService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 教程引用接口实现，属于教程与学习模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
@Service
@RequiredArgsConstructor
public class TutorialReferenceApiImpl implements TutorialReferenceApi {
    /**
     * tutorials。
     * 协作类型：<code>TutorialMapper</code>。
     */
    private final TutorialMapper tutorials;
    /**
     * 发布。
     * 协作类型：<code>TutorialPublicationService</code>。
     */
    private final TutorialPublicationService publication;

    /**
     * 判断是否存在教程引用。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>tutorials.selectById</code>。
     *
     * @param tutorialId 教程标识；类型为 <code>Long</code>
     * @return 本方法条件是否成立的判断结果
     */
    @Override
    @Transactional(readOnly = true)
    public boolean exists(Long tutorialId) {
        return tutorialId != null && tutorials.selectById(tutorialId) != null;
    }

    /**
     * 已发布教程处理入口。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>tutorials.selectById</code>、<code>publication.publicTutorial</code>。
     *
     * @param tutorialId 教程标识；类型为 <code>Long</code>
     * @return 可选结果；没有匹配值时由空 Optional 表达
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<PublishedTutorial> publishedTutorial(Long tutorialId) {
        TutorialEntity row = tutorialId == null ? null : tutorials.selectById(tutorialId);
        if (row == null || !"PUBLISHED".equals(row.getPublicationStatus())
                || row.getPublishedRevisionId() == null) {
            return Optional.empty();
        }
        JsonNode snapshot = publication.publicTutorial(row.getSlug());
        return Optional.of(new PublishedTutorial(row.getId(), row.getSlug(), snapshot.path("title").asText(),
                snapshot.path("summary").asText(), null, row.getPublishedAt()));
    }
}
