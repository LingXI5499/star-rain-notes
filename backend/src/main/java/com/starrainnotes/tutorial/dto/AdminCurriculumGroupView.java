package com.starrainnotes.tutorial.dto;

import java.util.List;

/** One flat curriculum group and its direct chapters. */
public record AdminCurriculumGroupView(
        Long id,
        String title,
        Integer sortOrder,
        long chapterCount,
        long publishedChapterCount,
        List<AdminCurriculumChapterView> chapters) {
}
