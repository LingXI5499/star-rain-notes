/*
 * GrammarServiceImpl：英语学习模块的业务实现文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：ApiException、EnglishSearchContentChangedEvent、Course、CourseRequest、Curriculum、Lesson、LessonRequest。
 * 阅读入口：curriculum（curriculum处理入口）；lesson（lesson处理入口）；updateCourse（更新Course）；setCoursePublished（设置Course已发布）。
 */

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
import java.util.HashSet;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 语法业务服务实现，属于英语学习模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
@RequiredArgsConstructor
public class GrammarServiceImpl implements GrammarService {
    /**
     * 数据映射。
     * 协作类型：<code>GrammarMapper</code>。
     */
    private final GrammarMapper mapper;
    /**
     * 事件集合。
     */
    private final ApplicationEventPublisher events;

    /**
     * curriculum处理入口。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.course</code>、<code>mapper.sections</code>、<code>mapper.lessons</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     *
     * @param admin 后台；类型为 <code>boolean</code>
     * @return 类型为 <code>Curriculum</code> 的处理结果
     */
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

    /**
     * lesson处理入口。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.lessonById</code>、<code>mapper.publicLesson</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     *
     * @param identity 身份；类型为 <code>String</code>
     * @param admin 后台；类型为 <code>boolean</code>
     * @return 类型为 <code>Lesson</code> 的处理结果
     */
    @Override
    @Transactional(readOnly = true)
    public Lesson lesson(String identity, boolean admin) {
        Lesson lesson = admin ? mapper.lessonById(id(identity)) : mapper.publicLesson(identity);
        if (lesson == null) throw new ApiException("ENGLISH_GRAMMAR_LESSON_NOT_FOUND", "语法课时不存在", 404);
        return lesson;
    }

    /**
     * 更新Course。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.updateCourse</code>、<code>mapper.course</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>CourseRequest</code>
     * @return 类型为 <code>Course</code> 的处理结果
     */
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

    /**
     * 设置Course已发布。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.setCourseStatus</code>。
     *
     * @param published 已发布；类型为 <code>boolean</code>
     */
    @Override
    @Transactional
    public void setCoursePublished(boolean published) {
        mapper.setCourseStatus(published ? "PUBLISHED" : "WITHDRAWN");
        changed("ENGLISH_GRAMMAR_COURSE", 1L);
        changed("ENGLISH_GRAMMAR_LESSON", null);
    }

    /**
     * 创建区块。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.insertSection</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>SectionRequest</code>
     * @return 类型为 <code>Section</code> 的处理结果
     */
    @Override
    @Transactional
    public Section createSection(SectionRequest request) {
        lockStructure();
        validateSection(request);
        Section section = new Section();
        section.setTitle(request.getTitle().trim());
        section.setSortOrder(order(request.getSortOrder()));
        mapper.insertSection(section);
        return requiredSection(section.getId());
    }

    /**
     * 更新区块。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.updateSection</code>。
     *
     * @param sectionId 区块标识；类型为 <code>String</code>
     * @param request 本次操作的请求参数对象；类型为 <code>SectionRequest</code>
     * @return 类型为 <code>Section</code> 的处理结果
     */
    @Override
    @Transactional
    public Section updateSection(String sectionId, SectionRequest request) {
        lockStructure();
        validateSection(request);
        Section section = new Section();
        section.setId(id(sectionId));
        section.setTitle(request.getTitle().trim());
        section.setSortOrder(order(request.getSortOrder()));
        if (mapper.updateSection(section) == 0) sectionNotFound();
        return requiredSection(section.getId());
    }

    /**
     * 删除区块。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.deleteSection</code>。
     *
     * @param sectionId 区块标识；类型为 <code>String</code>
     */
    @Override
    @Transactional
    public void deleteSection(String sectionId) {
        lockStructure();
        long section = id(sectionId);
        requiredSection(section);
        mapper.deleteLessonsInSection(section);
        if (mapper.deleteSection(section) == 0) sectionNotFound();
        changed("ENGLISH_GRAMMAR_LESSON", null);
    }

    /**
     * 创建Lesson。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.insertLesson</code>、<code>mapper.updateLesson</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>LessonRequest</code>
     * @return 类型为 <code>Lesson</code> 的处理结果
     */
    @Override
    @Transactional
    public Lesson createLesson(LessonRequest request) {
        lockStructure();
        validateLesson(request);
        Lesson lesson = fromRequest(request);
        lesson.setSlug("new-" + UUID.randomUUID());
        mapper.insertLesson(lesson);
        lesson.setSlug("grammar-" + lesson.getId());
        mapper.updateLesson(lesson);
        return lesson(String.valueOf(lesson.getId()), true);
    }

    /**
     * 更新Lesson。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.updateLesson</code>。
     *
     * @param lessonId lesson标识；类型为 <code>String</code>
     * @param request 本次操作的请求参数对象；类型为 <code>LessonRequest</code>
     * @return 类型为 <code>Lesson</code> 的处理结果
     */
    @Override
    @Transactional
    public Lesson updateLesson(String lessonId, LessonRequest request) {
        lockStructure();
        validateLesson(request);
        Lesson existing = lesson(lessonId, true);
        Lesson lesson = fromRequest(request);
        lesson.setId(existing.getId());
        lesson.setSlug(existing.getSlug());
        if (mapper.updateLesson(lesson) == 0) lessonNotFound();
        changed("ENGLISH_GRAMMAR_LESSON", lesson.getId());
        return lesson(lessonId, true);
    }

    /**
     * 设置Lesson已发布。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.setLessonStatus</code>。
     *
     * @param lessonId lesson标识；类型为 <code>String</code>
     * @param published 已发布；类型为 <code>boolean</code>
     * @return 类型为 <code>Lesson</code> 的处理结果
     */
    @Override
    @Transactional
    public Lesson setLessonPublished(String lessonId, boolean published) {
        if (mapper.setLessonStatus(id(lessonId), published ? "PUBLISHED" : "WITHDRAWN") == 0)
            lessonNotFound();
        changed("ENGLISH_GRAMMAR_LESSON", id(lessonId));
        return lesson(lessonId, true);
    }

    /**
     * 删除Lesson。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.deleteLesson</code>。
     *
     * @param lessonId lesson标识；类型为 <code>String</code>
     */
    @Override
    @Transactional
    public void deleteLesson(String lessonId) {
        lockStructure();
        if (mapper.deleteLesson(id(lessonId)) == 0) lessonNotFound();
        changed("ENGLISH_GRAMMAR_LESSON", id(lessonId));
    }

    /**
     * changed处理入口。
     * 主要协作调用：<code>events.publishEvent</code>。
     *
     * @param type 类型；类型为 <code>String</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     */
    private void lockStructure() {
        if (mapper.lockCourse() == null) {
            throw new ApiException("ENGLISH_GRAMMAR_NOT_FOUND", "语法课程不存在", 404);
        }
    }

    @Override
    @Transactional
    public void reorderSections(List<String> ids) {
        lockStructure();
        List<Long> ordered = validateOrder(ids, mapper.sections().stream().map(Section::getId).toList());
        for (int index = 0; index < ordered.size(); index++) mapper.orderSection(ordered.get(index), (index + 1) * 10);
    }

    @Override
    @Transactional
    public void reorderLessons(String sectionId, List<String> ids) {
        lockStructure();
        long section = id(sectionId);
        Section current = requiredSection(section);
        List<Long> ordered = validateOrder(ids, current.getLessons().stream().map(Lesson::getId).toList());
        for (int index = 0; index < ordered.size(); index++) mapper.orderLesson(ordered.get(index), section, (index + 1) * 10);
    }

    private List<Long> validateOrder(List<String> ids, List<Long> current) {
        if (ids == null || ids.size() != current.size() || ids.size() > 10000 || ids.stream().anyMatch(value -> value == null || !value.matches("[1-9][0-9]*"))) {
            throw new ApiException("ENGLISH_GRAMMAR_ORDER_INVALID", "请使用完整的同级目录排序", 400);
        }
        List<Long> ordered = ids.stream().map(this::id).toList();
        if (new HashSet<>(ordered).size() != ordered.size() || !new HashSet<>(ordered).equals(new HashSet<>(current))) {
            throw new ApiException("ENGLISH_GRAMMAR_ORDER_INVALID", "目录已变化，请刷新后重新排序", 409);
        }
        return ordered;
    }

    private void changed(String type, Long id) {
        events.publishEvent(new EnglishSearchContentChangedEvent(type, id));
    }

    /**
     * 必需区块处理入口。
     * 主要协作调用：<code>mapper.section</code>、<code>mapper.lessons</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     * @return 类型为 <code>Section</code> 的处理结果
     */
    private Section requiredSection(long id) {
        Section section = mapper.section(id);
        if (section == null) sectionNotFound();
        section.setLessons(mapper.lessons(id, false));
        return section;
    }

    /**
     * 从输入构造请求。
     * 关键处理步骤调用 <code>lesson.setSectionId</code>、<code>id</code>、<code>request.getSectionId</code>、<code>lesson.setTitle</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>LessonRequest</code>
     * @return 类型为 <code>Lesson</code> 的处理结果
     */
    private Lesson fromRequest(LessonRequest request) {
        Lesson lesson = new Lesson();
        lesson.setSectionId(id(request.getSectionId()));
        lesson.setTitle(request.getTitle().trim());
        lesson.setSummary(request.getSummary());
        lesson.setBodyMarkdown(request.getBodyMarkdown() == null ? "" : request.getBodyMarkdown());
        lesson.setSortOrder(order(request.getSortOrder()));
        return lesson;
    }

    /**
     * 校验区块。
     * 关键处理步骤调用 <code>request.getTitle</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>SectionRequest</code>
     */
    private void validateSection(SectionRequest request) {
        if (request == null || request.getTitle() == null || request.getTitle().isBlank())
            throw new ApiException("ENGLISH_GRAMMAR_INVALID", "请填写章节标题", 400);
    }

    /**
     * 校验Lesson。
     * 主要协作调用：<code>mapper.section</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>LessonRequest</code>
     */
    private void validateLesson(LessonRequest request) {
        if (request == null || request.getSectionId() == null || request.getTitle() == null
                || request.getTitle().isBlank())
            throw new ApiException("ENGLISH_GRAMMAR_INVALID", "请填写章节和课时标题", 400);
        if (mapper.section(id(request.getSectionId())) == null) sectionNotFound();
    }

    /**
     * 区块不存在处理入口。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     */
    private void sectionNotFound() {
        throw new ApiException("ENGLISH_GRAMMAR_SECTION_NOT_FOUND", "语法章节不存在", 404);
    }

    /**
     * lesson不存在处理入口。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     */
    private void lessonNotFound() {
        throw new ApiException("ENGLISH_GRAMMAR_LESSON_NOT_FOUND", "语法课时不存在", 404);
    }

    /**
     * 当前业务对象的数据库标识处理入口。
     * 关键处理步骤调用 <code>Long.parseLong</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     *
     * @param value 值；类型为 <code>String</code>
     * @return 类型为 <code>long</code> 的处理结果
     */
    private long id(String value) {
        try { return Long.parseLong(value); }
        catch (NumberFormatException exception) {
            throw new ApiException("ENGLISH_ID_INVALID", "无效的内容编号", 400);
        }
    }

    /**
     * 顺序处理入口。
     *
     * @param value 值；类型为 <code>Integer</code>
     * @return 类型为 <code>int</code> 的处理结果
     */
    private int order(Integer value) { return value == null ? 0 : value; }
}
