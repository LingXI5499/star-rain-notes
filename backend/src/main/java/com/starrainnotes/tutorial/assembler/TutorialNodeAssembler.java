package com.starrainnotes.tutorial.assembler;

import com.starrainnotes.site.service.SiteSettingsTimezone;
import com.starrainnotes.tutorial.dto.AdminCurriculumChapterView;
import com.starrainnotes.tutorial.dto.AdminTreeNodeView;
import com.starrainnotes.tutorial.dto.ChapterDetailView;
import com.starrainnotes.tutorial.entity.TutorialNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

/** Explicit API mapping for tutorial curriculum rows. */
@Component
@RequiredArgsConstructor
public class TutorialNodeAssembler {
    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private final SiteSettingsTimezone timezone;

    public AdminTreeNodeView toTreeNode(TutorialNode node) {
        return new AdminTreeNodeView(node.getId(), node.getParentId(), node.getNodeType(), node.getTitle(), node.getSlug(),
                node.getPublishStatus(), node.getSortOrder(), new ArrayList<>());
    }

    public AdminCurriculumChapterView toCurriculumChapter(TutorialNode chapter) {
        return new AdminCurriculumChapterView(chapter.getId(), chapter.getParentId(), chapter.getTitle(), chapter.getSlug(),
                chapter.getPublishStatus(), chapter.getSortOrder(), format(chapter.getUpdatedAt()));
    }

    public ChapterDetailView toChapterDetail(TutorialNode chapter) {
        return new ChapterDetailView(chapter.getId(), chapter.getTutorialId(), chapter.getParentId(), chapter.getNodeType(),
                chapter.getTitle(), chapter.getSlug(), chapter.getSummary(), chapter.getBodyMarkdown(), chapter.getPublishStatus(),
                chapter.getSortOrder(), format(chapter.getPublishedAt()), format(chapter.getUpdatedAt()));
    }

    private String format(LocalDateTime utc) { return utc == null ? null : timezone.atSite(utc).format(ISO_OFFSET); }
}
