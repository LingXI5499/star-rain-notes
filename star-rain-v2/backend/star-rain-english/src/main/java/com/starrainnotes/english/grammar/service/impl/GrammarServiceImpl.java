package com.starrainnotes.english.grammar.service.impl;

import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.english.api.event.EnglishSearchContentChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
import com.starrainnotes.english.grammar.dto.GrammarDto.Course;
import com.starrainnotes.english.grammar.dto.GrammarDto.CourseRequest;
import com.starrainnotes.english.grammar.dto.GrammarDto.Curriculum;
import com.starrainnotes.english.grammar.dto.GrammarDto.Lesson;
import com.starrainnotes.english.grammar.dto.GrammarDto.LessonRequest;
import com.starrainnotes.english.grammar.dto.GrammarDto.Section;
import com.starrainnotes.english.grammar.dto.GrammarDto.SectionRequest;
import com.starrainnotes.english.grammar.mapper.GrammarMapper;
import com.starrainnotes.english.grammar.service.GrammarService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GrammarServiceImpl implements GrammarService {
    private final GrammarMapper mapper;
    private final ApplicationEventPublisher events;

    @Override
    @Transactional(readOnly = true)
    public Curriculum curriculum(boolean admin) {
        Course course = mapper.course();
        if (course == null || (!admin && !"PUBLISHED".equals(course.getPublishStatus())))
            throw new ApiException("ENGLISH_GRAMMAR_NOT_FOUND", "语法课程尚未开放", 404);
        List<Section> sections = mapper.sections();
        for (Section section : sections) {
            section.setLessons(mapper.lessons(section.getId(), !admin));
        }
        return new Curriculum(course, sections);
    }

    @Override
    @Transactional(readOnly = true)
    public Lesson lesson(String identity, boolean admin) {
        Lesson lesson = admin ? mapper.lessonById(id(identity)) : mapper.publicLesson(identity);
        if (lesson == null) throw new ApiException("ENGLISH_GRAMMAR_LESSON_NOT_FOUND", "语法课时不存在", 404);
        return lesson;
    }

    @Override
    @Transactional
    public Course updateCourse(CourseRequest request) {
        if (request == null || request.getTitle() == null || request.getTitle().isBlank())
            throw new ApiException("ENGLISH_GRAMMAR_INVALID", "请填写课程标题", 400);
        Course course = new Course();
        course.setTitle(request.getTitle().trim());
        course.setSubtitle(request.getSubtitle());
        course.setSummary(request.getSummary());
        course.setIntroduction(request.getIntroduction());
        course.setRoadmapMarkdown(request.getRoadmapMarkdown());
        mapper.updateCourse(course);
        changed("ENGLISH_GRAMMAR_COURSE", 1L);
        return mapper.course();
    }

    @Override
    @Transactional
    public void setCoursePublished(boolean published) {
        mapper.setCourseStatus(published ? "PUBLISHED" : "WITHDRAWN");
        changed("ENGLISH_GRAMMAR_COURSE", 1L);
        changed("ENGLISH_GRAMMAR_LESSON", null);
    }

    @Override
    @Transactional
    public Section createSection(SectionRequest request) {
        validateSection(request);
        Section section = new Section();
        section.setTitle(request.getTitle().trim());
        section.setSortOrder(order(request.getSortOrder()));
        mapper.insertSection(section);
        return requiredSection(section.getId());
    }

    @Override
    @Transactional
    public Section updateSection(String sectionId, SectionRequest request) {
        validateSection(request);
        Section section = new Section();
        section.setId(id(sectionId));
        section.setTitle(request.getTitle().trim());
        section.setSortOrder(order(request.getSortOrder()));
        if (mapper.updateSection(section) == 0) sectionNotFound();
        return requiredSection(section.getId());
    }

    @Override
    @Transactional
    public void deleteSection(String sectionId) {
        if (mapper.deleteSection(id(sectionId)) == 0) sectionNotFound();
        changed("ENGLISH_GRAMMAR_LESSON", null);
    }

    @Override
    @Transactional
    public Lesson createLesson(LessonRequest request) {
        validateLesson(request);
        Lesson lesson = fromRequest(request);
        lesson.setSlug("new-" + UUID.randomUUID());
        mapper.insertLesson(lesson);
        lesson.setSlug("grammar-" + lesson.getId());
        mapper.updateLesson(lesson);
        return lesson(String.valueOf(lesson.getId()), true);
    }

    @Override
    @Transactional
    public Lesson updateLesson(String lessonId, LessonRequest request) {
        validateLesson(request);
        Lesson existing = lesson(lessonId, true);
        Lesson lesson = fromRequest(request);
        lesson.setId(existing.getId());
        lesson.setSlug(existing.getSlug());
        if (mapper.updateLesson(lesson) == 0) lessonNotFound();
        changed("ENGLISH_GRAMMAR_LESSON", lesson.getId());
        return lesson(lessonId, true);
    }

    @Override
    @Transactional
    public Lesson setLessonPublished(String lessonId, boolean published) {
        if (mapper.setLessonStatus(id(lessonId), published ? "PUBLISHED" : "WITHDRAWN") == 0)
            lessonNotFound();
        changed("ENGLISH_GRAMMAR_LESSON", id(lessonId));
        return lesson(lessonId, true);
    }

    @Override
    @Transactional
    public void deleteLesson(String lessonId) {
        if (mapper.deleteLesson(id(lessonId)) == 0) lessonNotFound();
        changed("ENGLISH_GRAMMAR_LESSON", id(lessonId));
    }

    private void changed(String type, Long id) {
        events.publishEvent(new EnglishSearchContentChangedEvent(type, id));
    }

    private Section requiredSection(long id) {
        Section section = mapper.section(id);
        if (section == null) sectionNotFound();
        section.setLessons(mapper.lessons(id, false));
        return section;
    }

    private Lesson fromRequest(LessonRequest request) {
        Lesson lesson = new Lesson();
        lesson.setSectionId(id(request.getSectionId()));
        lesson.setTitle(request.getTitle().trim());
        lesson.setSummary(request.getSummary());
        lesson.setBodyMarkdown(request.getBodyMarkdown() == null ? "" : request.getBodyMarkdown());
        lesson.setSortOrder(order(request.getSortOrder()));
        return lesson;
    }

    private void validateSection(SectionRequest request) {
        if (request == null || request.getTitle() == null || request.getTitle().isBlank())
            throw new ApiException("ENGLISH_GRAMMAR_INVALID", "请填写章节标题", 400);
    }

    private void validateLesson(LessonRequest request) {
        if (request == null || request.getSectionId() == null || request.getTitle() == null
                || request.getTitle().isBlank())
            throw new ApiException("ENGLISH_GRAMMAR_INVALID", "请填写章节和课时标题", 400);
        if (mapper.section(id(request.getSectionId())) == null) sectionNotFound();
    }

    private void sectionNotFound() {
        throw new ApiException("ENGLISH_GRAMMAR_SECTION_NOT_FOUND", "语法章节不存在", 404);
    }

    private void lessonNotFound() {
        throw new ApiException("ENGLISH_GRAMMAR_LESSON_NOT_FOUND", "语法课时不存在", 404);
    }

    private long id(String value) {
        try { return Long.parseLong(value); }
        catch (NumberFormatException exception) {
            throw new ApiException("ENGLISH_ID_INVALID", "无效的内容编号", 400);
        }
    }

    private int order(Integer value) { return value == null ? 0 : value; }
}
