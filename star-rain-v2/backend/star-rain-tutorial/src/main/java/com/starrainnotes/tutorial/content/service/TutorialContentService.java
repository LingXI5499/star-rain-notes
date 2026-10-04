package com.starrainnotes.tutorial.content.service;

import com.starrainnotes.common.result.PageResult;
import com.starrainnotes.tutorial.content.dto.ChapterBodyDTO;
import com.starrainnotes.tutorial.content.dto.ChapterCreateDTO;
import com.starrainnotes.tutorial.content.dto.ChapterUpdateDTO;
import com.starrainnotes.tutorial.content.dto.TutorialCreateDTO;
import com.starrainnotes.tutorial.content.dto.TutorialUpdateDTO;
import com.starrainnotes.tutorial.content.vo.TutorialAdminVO;
import com.starrainnotes.tutorial.content.vo.TutorialCategoryVO;
import com.starrainnotes.tutorial.content.vo.TutorialChapterVO;
import com.starrainnotes.tutorial.content.vo.TutorialCurriculumVO;
import com.starrainnotes.tutorial.content.vo.TutorialGroupVO;
import java.util.List;

public interface TutorialContentService {
    List<TutorialCategoryVO> categories();
    TutorialCategoryVO createCategory(String name);
    TutorialCategoryVO updateCategory(Long id, String name);
    void deleteCategory(Long id);
    void reorderCategories(List<Long> ids);

    PageResult<TutorialAdminVO> tutorials(int page, int pageSize);
    TutorialAdminVO tutorial(Long id);
    TutorialAdminVO createTutorial(TutorialCreateDTO request);
    TutorialAdminVO updateTutorial(Long id, TutorialUpdateDTO request);
    void deleteTutorial(Long id);
    void reorderTutorials(Long categoryId, List<Long> ids);

    TutorialCurriculumVO curriculum(Long tutorialId);
    TutorialGroupVO createGroup(Long tutorialId, String title);
    TutorialGroupVO updateGroup(Long groupId, String title);
    void archiveGroup(Long groupId);
    void restoreGroup(Long groupId);
    void reorderGroups(Long tutorialId, List<Long> ids);

    TutorialChapterVO chapter(Long chapterId);
    TutorialChapterVO createChapter(Long groupId, ChapterCreateDTO request);
    TutorialChapterVO updateChapter(Long chapterId, ChapterUpdateDTO request);
    TutorialChapterVO updateChapterBody(Long chapterId, ChapterBodyDTO request);
    void archiveChapter(Long chapterId);
    void restoreChapter(Long chapterId);
    void moveChapter(Long chapterId, Long groupId);
    void reorderChapters(Long groupId, List<Long> ids);
}
