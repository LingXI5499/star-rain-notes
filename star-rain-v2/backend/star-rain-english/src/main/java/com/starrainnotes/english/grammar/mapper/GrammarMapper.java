package com.starrainnotes.english.grammar.mapper;

import com.starrainnotes.english.grammar.dto.GrammarDto.Course;
import com.starrainnotes.english.grammar.dto.GrammarDto.Lesson;
import com.starrainnotes.english.grammar.dto.GrammarDto.Section;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GrammarMapper {
    Course course();
    List<Section> sections();
    Section section(@Param("id") long id);
    List<Lesson> lessons(@Param("sectionId") long sectionId, @Param("publicOnly") boolean publicOnly);
    Lesson lessonById(@Param("id") long id);
    Lesson publicLesson(@Param("slug") String slug);
    int updateCourse(Course course);
    int setCourseStatus(@Param("status") String status);
    int insertSection(Section section);
    int updateSection(Section section);
    int deleteSection(@Param("id") long id);
    int insertLesson(Lesson lesson);
    int updateLesson(Lesson lesson);
    int setLessonStatus(@Param("id") long id, @Param("status") String status);
    int deleteLesson(@Param("id") long id);
}
