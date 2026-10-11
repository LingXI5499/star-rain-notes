/*
 * GrammarMapper：英语学习模块的数据访问文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 主要协作类型：Course、Lesson、Section。
 * 阅读入口：course（course处理入口）；sections（sections处理入口）；section（区块处理入口）；lessons（lessons处理入口）。
 */

package com.starrainnotes.english.grammar.mapper;

import com.starrainnotes.english.grammar.dto.GrammarDto.Course;
import com.starrainnotes.english.grammar.dto.GrammarDto.Lesson;
import com.starrainnotes.english.grammar.dto.GrammarDto.Section;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 语法数据映射，属于英语学习模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
@Mapper
public interface GrammarMapper {
    /**
     * course处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_grammar_course</code>。
     * @return 类型为 <code>Course</code> 的处理结果
     */
    Course course();
    /**
     * sections处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_grammar_section</code>。
     * @return 本次处理得到的条目集合
     */
    List<Section> sections();
    /**
     * 区块处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_grammar_section</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>Section</code> 的处理结果
     */
    Section section(@Param("id") long id);
    /**
     * lessons处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_grammar_lesson</code>。
     *
     * @param sectionId 区块标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("sectionId")</code>
     * @param publicOnly 公开Only；类型为 <code>boolean</code>；对应 MyBatis 命名参数 <code>@Param("publicOnly")</code>
     * @return 本次处理得到的条目集合
     */
    List<Lesson> lessons(@Param("sectionId") long sectionId, @Param("publicOnly") boolean publicOnly);
    /**
     * 按标识读取lesson。
     *
     * 持久化操作：读取，涉及 <code>sr_english_grammar_lesson</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return 类型为 <code>Lesson</code> 的处理结果
     */
    Lesson lessonById(@Param("id") long id);
    /**
     * 公开Lesson处理入口。
     *
     * 持久化操作：读取，涉及 <code>sr_english_grammar_lesson</code>。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("slug")</code>
     * @return 类型为 <code>Lesson</code> 的处理结果
     */
    Lesson publicLesson(@Param("slug") String slug);
    /**
     * 更新Course。
     *
     * 持久化操作：更新，涉及 <code>sr_english_grammar_course</code>。
     *
     * @param course course；类型为 <code>Course</code>
     * @return SQL 写入影响的记录行数
     */
    int updateCourse(Course course);
    /**
     * 设置Course状态。
     *
     * 持久化操作：更新，涉及 <code>sr_english_grammar_course</code>。
     *
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("status")</code>
     * @return SQL 写入影响的记录行数
     */
    int setCourseStatus(@Param("status") String status);
    /**
     * 写入区块。
     *
     * 持久化操作：新增，涉及 <code>sr_english_grammar_section</code>。
     *
     * @param section 区块；类型为 <code>Section</code>
     * @return SQL 写入影响的记录行数
     */
    int insertSection(Section section);
    /**
     * 更新区块。
     *
     * 持久化操作：更新，涉及 <code>sr_english_grammar_section</code>。
     *
     * @param section 区块；类型为 <code>Section</code>
     * @return SQL 写入影响的记录行数
     */
    int updateSection(Section section);
    /**
     * 删除区块。
     *
     * 持久化操作：删除，涉及 <code>sr_english_grammar_section</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return SQL 写入影响的记录行数
     */
    int deleteSection(@Param("id") long id);
    /**
     * 写入Lesson。
     *
     * 持久化操作：新增，涉及 <code>sr_english_grammar_lesson</code>。
     *
     * @param lesson lesson；类型为 <code>Lesson</code>
     * @return SQL 写入影响的记录行数
     */
    int insertLesson(Lesson lesson);
    /**
     * 更新Lesson。
     *
     * 持久化操作：更新，涉及 <code>sr_english_grammar_lesson</code>。
     *
     * @param lesson lesson；类型为 <code>Lesson</code>
     * @return SQL 写入影响的记录行数
     */
    int updateLesson(Lesson lesson);
    /**
     * 设置Lesson状态。
     *
     * 持久化操作：更新，涉及 <code>sr_english_grammar_lesson</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>String</code>；对应 MyBatis 命名参数 <code>@Param("status")</code>
     * @return SQL 写入影响的记录行数
     */
    int setLessonStatus(@Param("id") long id, @Param("status") String status);
    /**
     * 删除Lesson。
     *
     * 持久化操作：删除，涉及 <code>sr_english_grammar_lesson</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
     * @return SQL 写入影响的记录行数
     */
    int deleteLesson(@Param("id") long id);
}
