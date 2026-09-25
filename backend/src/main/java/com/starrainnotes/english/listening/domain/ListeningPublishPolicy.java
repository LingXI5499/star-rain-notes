package com.starrainnotes.english.listening.domain;

import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.listening.entity.ListeningItem;
import org.springframework.http.HttpStatus;
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

    public void requireLevel(Integer level) {
        if (level == null || level < 1 || level > 3) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_LISTENING_LEVEL_INVALID",
                    "Invalid listening level", "Level must be 1, 2 or 3.");
        }
    }

    public void requireDeletable(String publishStatus) {
        if ("PUBLISHED".equals(publishStatus)) {
            throw new ApiException(HttpStatus.CONFLICT, "ENGLISH_LISTENING_PUBLISHED_DELETE_FORBIDDEN",
                    "Published item cannot be deleted", "Withdraw the item before deleting it.");
        }
    }

    public void requireWithdrawable(String publishStatus) {
        if ("DRAFT".equals(publishStatus)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_INVALID_PUBLISH_TRANSITION",
                    "Invalid publish transition", "A draft item cannot be withdrawn.");
        }
    }

    public void requireKnownCefr(String cefr, boolean exists) {
        if (cefr == null || exists) return;
        throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_CEFR_INVALID",
                "Invalid CEFR level", "The selected CEFR level does not exist.");
    }

    public void requireAudio(Long mediaId, boolean audio) {
        if (mediaId == null || audio) return;
        throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_AUDIO_MEDIA_INVALID",
                "Invalid audio", "The selected audio must be an AUDIO asset.");
    }

    public void requireCover(Long mediaId, boolean image) {
        if (mediaId == null || image) return;
        throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_COVER_MEDIA",
                "Invalid cover", "The selected cover must be an IMAGE asset.");
    }

    public void requireEnabledDimension(String expected, String actual) {
        if (actual == null) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_TAXONOMY_NOT_FOUND",
                    "Invalid taxonomy term", "The selected tag does not exist or is disabled.");
        }
        if (!expected.equals(actual)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_TAXONOMY_DEPTH_INVALID",
                    "Invalid tag dimension",
                    "A " + expected + " tag cannot reference the '" + actual + "' dimension.");
        }
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
}
