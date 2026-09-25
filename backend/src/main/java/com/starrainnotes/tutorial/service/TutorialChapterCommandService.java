package com.starrainnotes.tutorial.service;

import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.common.slug.NumericSlugGenerator;
import com.starrainnotes.tutorial.assembler.TutorialNodeAssembler;
import com.starrainnotes.tutorial.dto.ChapterDetailView;
import com.starrainnotes.tutorial.dto.CreateChapterRequest;
import com.starrainnotes.tutorial.dto.MoveIndexRequest;
import com.starrainnotes.tutorial.dto.ReassignChapterRequest;
import com.starrainnotes.tutorial.dto.UpdateChapterRequest;
import com.starrainnotes.tutorial.entity.Tutorial;
import com.starrainnotes.tutorial.entity.TutorialNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/** Chapter commands for the fixed two-level curriculum. */
@Service
@RequiredArgsConstructor
public class TutorialChapterCommandService {
    private static final String PUBLISHED = "PUBLISHED";
    private static final String DRAFT = "DRAFT";
    private static final String WITHDRAWN = "WITHDRAWN";

    private final TutorialNodeAccess access;
    private final TutorialNodeAssembler nodeAssembler;

    @Transactional
    public ChapterDetailView createChapter(Long tutorialId, CreateChapterRequest request) {
        access.requireTutorial(tutorialId);
        TutorialNode group = access.requireTargetGroup(tutorialId, request.groupId());
        String slug = NumericSlugGenerator.forCreate(request.slug(),
                candidate -> access.chapterSlugExists(tutorialId, candidate, null));
        access.assertChapterSlugFree(tutorialId, slug, null);
        TutorialNode chapter = new TutorialNode();
        chapter.setTutorialId(tutorialId);
        chapter.setParentId(group.getId());
        chapter.setNodeType(TutorialNodeAccess.CHAPTER);
        chapter.setTitle(request.title());
        chapter.setSlug(slug);
        chapter.setSummary(request.summary());
        chapter.setBodyMarkdown(request.bodyMarkdown());
        chapter.setPublishStatus(DRAFT);
        chapter.setPublishedAt(null);
        chapter.setSortOrder(access.nextChapterOrder(tutorialId, group.getId()));
        access.nodes().insert(chapter);
        return nodeAssembler.toChapterDetail(chapter);
    }

    @Transactional
    public ChapterDetailView updateChapter(Long tutorialId, Long chapterId, UpdateChapterRequest request) {
        TutorialNode chapter = access.requireChapter(tutorialId, chapterId);
        String slug = NumericSlugGenerator.forUpdate(request.slug(), chapter.getSlug());
        access.assertChapterSlugFree(tutorialId, slug, chapterId);
        chapter.setTitle(request.title());
        chapter.setSlug(slug);
        chapter.setSummary(request.summary());
        chapter.setBodyMarkdown(request.bodyMarkdown());
        access.nodes().updateById(chapter);
        return nodeAssembler.toChapterDetail(chapter);
    }

    @Transactional
    public void deleteChapter(Long tutorialId, Long chapterId) {
        TutorialNode chapter = access.requireChapter(tutorialId, chapterId);
        Long groupId = chapter.getParentId();
        access.nodes().deleteById(chapterId);
        access.normalizeOrders(access.loadChapters(tutorialId, groupId));
    }

    @Transactional
    public ChapterDetailView publishChapter(Long tutorialId, Long chapterId) {
        TutorialNode chapter = access.requireChapter(tutorialId, chapterId);
        Tutorial tutorial = access.requireTutorial(tutorialId);
        if (!PUBLISHED.equals(tutorial.getPublishStatus())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "TUTORIAL_NOT_PUBLISHED",
                    "Tutorial not published", "Chapters can only be published while their tutorial is PUBLISHED.");
        }
        if (!PUBLISHED.equals(chapter.getPublishStatus())) {
            if (chapter.getPublishedAt() == null) chapter.setPublishedAt(LocalDateTime.now(Clock.systemUTC()));
            chapter.setPublishStatus(PUBLISHED);
            access.nodes().updateById(chapter);
        }
        return nodeAssembler.toChapterDetail(chapter);
    }

    @Transactional
    public ChapterDetailView withdrawChapter(Long tutorialId, Long chapterId) {
        TutorialNode chapter = access.requireChapter(tutorialId, chapterId);
        if (DRAFT.equals(chapter.getPublishStatus())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_PUBLISH_TRANSITION",
                    "Cannot withdraw a draft", "Only published chapters can be withdrawn.");
        }
        if (!WITHDRAWN.equals(chapter.getPublishStatus())) {
            chapter.setPublishStatus(WITHDRAWN);
            access.nodes().updateById(chapter);
        }
        return nodeAssembler.toChapterDetail(chapter);
    }

    @Transactional
    public void moveChapter(Long tutorialId, Long chapterId, MoveIndexRequest request) {
        placeChapter(tutorialId, chapterId, request);
    }

    public void placeChapter(Long tutorialId, Long chapterId, MoveIndexRequest request) {
        TutorialNode chapter = access.requireChapter(tutorialId, chapterId);
        List<TutorialNode> chapters = access.loadChapters(tutorialId, chapter.getParentId());
        chapters.removeIf(item -> item.getId().equals(chapterId));
        chapters.add(Math.min(Math.max(request.targetIndex(), 0), chapters.size()), chapter);
        access.normalizeOrders(chapters);
    }

    @Transactional
    public void reassignChapter(Long tutorialId, Long chapterId, ReassignChapterRequest request) {
        TutorialNode chapter = access.requireChapter(tutorialId, chapterId);
        TutorialNode targetGroup = access.requireTargetGroup(tutorialId, request.targetGroupId());
        Long sourceGroupId = chapter.getParentId();
        if (sourceGroupId.equals(targetGroup.getId())) return;

        List<TutorialNode> source = access.loadChapters(tutorialId, sourceGroupId);
        source.removeIf(item -> item.getId().equals(chapterId));
        access.normalizeOrders(source);

        List<TutorialNode> target = access.loadChapters(tutorialId, targetGroup.getId());
        chapter.setParentId(targetGroup.getId());
        chapter.setSortOrder((target.size() + 1) * 10);
        access.nodes().updateById(chapter);
        target.add(chapter);
        access.normalizeOrders(target);
    }
}
