package com.starrainnotes.tutorial.service;

import com.starrainnotes.tutorial.dto.AdminTutorialDetailView;
import com.starrainnotes.tutorial.dto.PublicChapterView;
import com.starrainnotes.tutorial.dto.PublicTutorialDetailView;
import com.starrainnotes.tutorial.dto.PublicTutorialSummaryView;
import com.starrainnotes.tutorial.dto.TutorialPageView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** Read-side boundary for tutorial HTTP and cross-domain consumers. */
@Service
@RequiredArgsConstructor
public class TutorialQueryService {
    private final TutorialService tutorialService;

    public TutorialPageView adminList(int page, int pageSize, String status, String query, Long categoryId) {
        return tutorialService.adminList(page, pageSize, status, query, categoryId);
    }

    public AdminTutorialDetailView adminDetail(Long tutorialId) { return tutorialService.adminDetail(tutorialId); }

    public List<PublicTutorialSummaryView> publicList(String categorySlug) { return tutorialService.publicList(categorySlug); }

    public PublicTutorialDetailView publicDetail(String slug) { return tutorialService.publicDetail(slug); }

    public PublicChapterView publicChapter(String tutorialSlug, String chapterSlug) {
        return tutorialService.publicChapter(tutorialSlug, chapterSlug);
    }
}
