/*
 * GrammarService：英语学习模块的业务契约文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：Course、CourseRequest、Curriculum、Lesson、LessonRequest、Section、SectionRequest。
 * 阅读入口：curriculum（curriculum处理入口）；lesson（lesson处理入口）；updateCourse（更新Course）；setCoursePublished（设置Course已发布）。
 */

package com.starrainnotes.english.grammar.service;

import com.starrainnotes.english.grammar.dto.GrammarDto.Course;
import com.starrainnotes.english.grammar.dto.GrammarDto.CourseRequest;
import com.starrainnotes.english.grammar.dto.GrammarDto.Curriculum;
import com.starrainnotes.english.grammar.dto.GrammarDto.Lesson;
import com.starrainnotes.english.grammar.dto.GrammarDto.LessonRequest;
import com.starrainnotes.english.grammar.dto.GrammarDto.Section;
import com.starrainnotes.english.grammar.dto.GrammarDto.SectionRequest;

// 语法课程内容读写入口。
/**
 * 语法业务服务，属于英语学习模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface GrammarService {

    /**
     * curriculum处理入口。
     *
     * @param admin 后台；类型为 <code>boolean</code>
     * @return 类型为 <code>Curriculum</code> 的处理结果
     */
    Curriculum curriculum(boolean admin);

    /**
     * lesson处理入口。
     *
     * @param identity 身份；类型为 <code>String</code>
     * @param admin 后台；类型为 <code>boolean</code>
     * @return 类型为 <code>Lesson</code> 的处理结果
     */
    Lesson lesson(String identity, boolean admin);

    /**
     * 更新Course。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>CourseRequest</code>
     * @return 类型为 <code>Course</code> 的处理结果
     */
    Course updateCourse(CourseRequest request);

    /**
     * 设置Course已发布。
     *
     * @param published 已发布；类型为 <code>boolean</code>
     */
    void setCoursePublished(boolean published);

    /**
     * 创建区块。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>SectionRequest</code>
     * @return 类型为 <code>Section</code> 的处理结果
     */
    Section createSection(SectionRequest request);

    /**
     * 更新区块。
     *
     * @param sectionId 区块标识；类型为 <code>String</code>
     * @param request 本次操作的请求参数对象；类型为 <code>SectionRequest</code>
     * @return 类型为 <code>Section</code> 的处理结果
     */
    Section updateSection(String sectionId, SectionRequest request);

    /**
     * 删除区块。
     *
     * @param sectionId 区块标识；类型为 <code>String</code>
     */
    void deleteSection(String sectionId);

    /**
     * 创建Lesson。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>LessonRequest</code>
     * @return 类型为 <code>Lesson</code> 的处理结果
     */
    Lesson createLesson(LessonRequest request);

    /**
     * 更新Lesson。
     *
     * @param lessonId lesson标识；类型为 <code>String</code>
     * @param request 本次操作的请求参数对象；类型为 <code>LessonRequest</code>
     * @return 类型为 <code>Lesson</code> 的处理结果
     */
    Lesson updateLesson(String lessonId, LessonRequest request);

    /**
     * 设置Lesson已发布。
     *
     * @param lessonId lesson标识；类型为 <code>String</code>
     * @param published 已发布；类型为 <code>boolean</code>
     * @return 类型为 <code>Lesson</code> 的处理结果
     */
    Lesson setLessonPublished(String lessonId, boolean published);

    /**
     * 删除Lesson。
     *
     * @param lessonId lesson标识；类型为 <code>String</code>
     */
    void deleteLesson(String lessonId);
}
