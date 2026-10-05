package com.starrainnotes.english.grammar.service;

import com.starrainnotes.english.grammar.dto.GrammarDto.Course;
import com.starrainnotes.english.grammar.dto.GrammarDto.CourseRequest;
import com.starrainnotes.english.grammar.dto.GrammarDto.Curriculum;
import com.starrainnotes.english.grammar.dto.GrammarDto.Lesson;
import com.starrainnotes.english.grammar.dto.GrammarDto.LessonRequest;
import com.starrainnotes.english.grammar.dto.GrammarDto.Section;
import com.starrainnotes.english.grammar.dto.GrammarDto.SectionRequest;

// 语法课程内容读写入口。
public interface GrammarService {

    Curriculum curriculum(boolean admin);

    Lesson lesson(String identity, boolean admin);

    Course updateCourse(CourseRequest request);

    void setCoursePublished(boolean published);

    Section createSection(SectionRequest request);

    Section updateSection(String sectionId, SectionRequest request);

    void deleteSection(String sectionId);

    Lesson createLesson(LessonRequest request);

    Lesson updateLesson(String lessonId, LessonRequest request);

    Lesson setLessonPublished(String lessonId, boolean published);

    void deleteLesson(String lessonId);
}
