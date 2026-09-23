package com.starrainnotes.english.listening.domain;

import com.starrainnotes.english.listening.entity.ListeningItem;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Applies the existing publication prerequisites without changing their semantics. */
@Component
public class ListeningPublishPolicy {
    public List<String> problems(ListeningItem item, boolean hasSceneTag, boolean hasFormatTag,
                                 boolean validSegments, boolean hasPublishedExercise, boolean exercisesValid) {
        List<String> problems = new ArrayList<>();
        if (blank(item.getTitle())) problems.add("标题不能为空");
        if (blank(item.getSlug())) problems.add("slug 不能为空");
        if (blank(item.getSummary())) problems.add("摘要不能为空");
        if (item.getCefrLevel() == null) problems.add("CEFR 等级不能为空");
        if (item.getListeningLevel() == null || item.getListeningLevel() < 1 || item.getListeningLevel() > 3) {
            problems.add("能力层级必须为1/2/3");
        }
        if (item.getAudioMediaId() == null) problems.add("音频资源不能为空");
        if (!hasSceneTag) problems.add("至少需要一个场景(SCENE)标签");
        if (!hasFormatTag) problems.add("至少需要一个形式(FORMAT)标签");
        if (!validSegments) problems.add("至少需要一个包含原文、时间有效且互不重叠的片段");
        if (!hasPublishedExercise) problems.add("至少需要一道已发布听力练习");
        else if (!exercisesValid) problems.add("存在配置不合法的已发布练习");
        return problems;
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
}
