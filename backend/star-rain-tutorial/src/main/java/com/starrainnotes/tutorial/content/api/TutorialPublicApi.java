/*
 * TutorialPublicApi：教程与学习模块的跨模块接口文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 * 阅读入口：latestPublished（最新已发布处理入口）。
 */

package com.starrainnotes.tutorial.content.api;

import java.util.List;

/**
 * 教程公开接口，属于教程与学习模块。
 * 提供模块边界上的能力契约，让调用方依赖接口及传输类型，而不是读取本模块的内部实现。
 */
public interface TutorialPublicApi {
    /**
     * 最新已发布处理入口。
     *
     * @param limit 本次查询或处理的数量上限；类型为 <code>int</code>
     * @return 本次处理得到的条目集合
     */
    List<PublishedTutorial> latestPublished(int limit);
}
