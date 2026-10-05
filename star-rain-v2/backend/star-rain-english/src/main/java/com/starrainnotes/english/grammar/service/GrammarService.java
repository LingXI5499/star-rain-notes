package com.starrainnotes.english.grammar.service;

import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.english.grammar.dto.GrammarDto.Course;
import com.starrainnotes.english.grammar.dto.GrammarDto.CourseRequest;
import com.starrainnotes.english.grammar.dto.GrammarDto.Curriculum;
import com.starrainnotes.english.grammar.dto.GrammarDto.Lesson;
import com.starrainnotes.english.grammar.dto.GrammarDto.LessonRequest;
import com.starrainnotes.english.grammar.dto.GrammarDto.Section;
import com.starrainnotes.english.grammar.dto.GrammarDto.SectionRequest;
import com.starrainnotes.english.grammar.mapper.GrammarMapper;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GrammarService {
    private final GrammarMapper mapper;

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

    @Transactional(readOnly = true)
    public Lesson lesson(String identity, boolean admin) {
        Lesson lesson = admin ? mapper.lessonById(id(identity)) : mapper.publicLesson(identity);
        if (lesson == null) throw new ApiException("ENGLISH_GRAMMAR_LESSON_NOT_FOUND", "语法课时不存在", 404);
        return lesson;
    }

    @Transactional
    public Course updateCourse(CourseRequest request) {
        if (request == null || request.title() == null || request.title().isBlank())
            throw new ApiException("ENGLISH_GRAMMAR_INVALID", "请填写课程标题", 400);
        Course course = new Course();
        course.setTitle(request.title().trim());
        course.setSubtitle(request.subtitle());
        course.setSummary(request.summary());
        course.setIntroduction(request.introduction());
        course.setRoadmapMarkdown(request.roadmapMarkdown());
        mapper.updateCourse(course);
        return mapper.course();
    }

    @Transactional
    public void setCoursePublished(boolean published) {
        mapper.setCourseStatus(published ? "PUBLISHED" : "WITHDRAWN");
    }

    @Transactional
    public Section createSection(SectionRequest request) {
        validateSection(request);
        Section section = new Section();
        section.setTitle(request.title().trim());
        section.setSortOrder(order(request.sortOrder()));
        mapper.insertSection(section);
        return requiredSection(section.getId());
    }

    @Transactional
    public Section updateSection(String sectionId, SectionRequest request) {
        validateSection(request);
        Section section = new Section();
        section.setId(id(sectionId));
        section.setTitle(request.title().trim());
        section.setSortOrder(order(request.sortOrder()));
        if (mapper.updateSection(section) == 0) sectionNotFound();
        return requiredSection(section.getId());
    }

    @Transactional
    public void deleteSection(String sectionId) {
        if (mapper.deleteSection(id(sectionId)) == 0) sectionNotFound();
    }

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

    @Transactional
    public Lesson updateLesson(String lessonId, LessonRequest request) {
        validateLesson(request);
        Lesson existing = lesson(lessonId, true);
        Lesson lesson = fromRequest(request);
        lesson.setId(existing.getId());
        lesson.setSlug(existing.getSlug());
        if (mapper.updateLesson(lesson) == 0) lessonNotFound();
        return lesson(lessonId, true);
    }

    @Transactional
    public Lesson setLessonPublished(String lessonId, boolean published) {
        if (mapper.setLessonStatus(id(lessonId), published ? "PUBLISHED" : "WITHDRAWN") == 0)
            lessonNotFound();
        return lesson(lessonId, true);
    }

    @Transactional
    public void deleteLesson(String lessonId) {
        if (mapper.deleteLesson(id(lessonId)) == 0) lessonNotFound();
    }

    private Section requiredSection(long id) {
        Section section = mapper.section(id);
        if (section == null) sectionNotFound();
        section.setLessons(mapper.lessons(id, false));
        return section;
    }

    private Lesson fromRequest(LessonRequest request) {
        Lesson lesson = new Lesson();
        lesson.setSectionId(id(request.sectionId()));
        lesson.setTitle(request.title().trim());
        lesson.setSummary(request.summary());
        lesson.setBodyMarkdown(request.bodyMarkdown() == null ? "" : request.bodyMarkdown());
        lesson.setSortOrder(order(request.sortOrder()));
        return lesson;
    }

    private void validateSection(SectionRequest request) {
        if (request == null || request.title() == null || request.title().isBlank())
            throw new ApiException("ENGLISH_GRAMMAR_INVALID", "请填写章节标题", 400);
    }

    private void validateLesson(LessonRequest request) {
        if (request == null || request.sectionId() == null || request.title() == null
                || request.title().isBlank())
            throw new ApiException("ENGLISH_GRAMMAR_INVALID", "请填写章节和课时标题", 400);
        if (mapper.section(id(request.sectionId())) == null) sectionNotFound();
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
