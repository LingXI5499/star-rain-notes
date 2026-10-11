/*
 * TutorialReferenceApi：教程与学习模块的跨模块接口文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 * 阅读入口：exists（判断是否存在教程引用）；publishedTutorial（已发布教程处理入口）。
 */

package com.starrainnotes.tutorial.content.api;

import java.util.Optional;

/**
 * 教程引用接口，属于教程与学习模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
public interface TutorialReferenceApi {
    /**
     * 判断是否存在教程引用。
     *
     * @param tutorialId 教程标识；类型为 <code>Long</code>
     * @return 本方法条件是否成立的判断结果
     */
    boolean exists(Long tutorialId);
    /**
     * 已发布教程处理入口。
     *
     * @param tutorialId 教程标识；类型为 <code>Long</code>
     * @return 可选结果；没有匹配值时由空 Optional 表达
     */
    Optional<PublishedTutorial> publishedTutorial(Long tutorialId);
}
