package com.starrainnotes.tutorial.content.mapper;

import com.starrainnotes.tutorial.content.entity.TutorialQuestionEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/*
 * 章节简答题的全部 SQL 都在 mapper/tutorial/TutorialQuestionMapper.xml。
 * 本接口不继承 BaseMapper，每个方法在 XML 里都有唯一对应的语句。
 */
@Mapper
public interface TutorialQuestionMapper {

    int insert(TutorialQuestionEntity question);

    TutorialQuestionEntity selectById(@Param("id") Long id);

    List<TutorialQuestionEntity> listByChapterId(@Param("chapterId") Long chapterId);

    /* 发布冻结快照只取 ENABLED 题目。 */
    List<TutorialQuestionEntity> listEnabledByChapterId(@Param("chapterId") Long chapterId);

    int updateContent(@Param("id") Long id, @Param("questionText") String questionText,
                      @Param("referenceAnswer") String referenceAnswer, @Param("status") String status,
                      @Param("updatedAt") LocalDateTime updatedAt);

    /* 排序只改 sort_order，与原先 updateById 的字段范围一致（不碰 updated_at）。 */
    int updateSortOrder(@Param("id") Long id, @Param("sortOrder") int sortOrder);

    int deleteById(@Param("id") Long id);
}
