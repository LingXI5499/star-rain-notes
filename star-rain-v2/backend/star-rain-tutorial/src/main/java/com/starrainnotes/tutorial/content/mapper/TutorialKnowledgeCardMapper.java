package com.starrainnotes.tutorial.content.mapper;

import com.starrainnotes.tutorial.content.entity.TutorialKnowledgeCardEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/*
 * 章节知识卡片的全部 SQL 都在 mapper/tutorial/TutorialKnowledgeCardMapper.xml。
 * 本接口不继承 BaseMapper，每个方法在 XML 里都有唯一对应的语句。
 */
@Mapper
public interface TutorialKnowledgeCardMapper {

    int insert(TutorialKnowledgeCardEntity card);

    TutorialKnowledgeCardEntity selectById(@Param("id") Long id);

    List<TutorialKnowledgeCardEntity> listByChapterId(@Param("chapterId") Long chapterId);

    /* 发布冻结快照只取 ENABLED 卡片。 */
    List<TutorialKnowledgeCardEntity> listEnabledByChapterId(@Param("chapterId") Long chapterId);

    int updateContent(@Param("id") Long id, @Param("frontText") String frontText,
                      @Param("backMarkdown") String backMarkdown, @Param("status") String status,
                      @Param("updatedAt") LocalDateTime updatedAt);

    /* 排序只改 sort_order，与原先 updateById 的字段范围一致（不碰 updated_at）。 */
    int updateSortOrder(@Param("id") Long id, @Param("sortOrder") int sortOrder);

    int deleteById(@Param("id") Long id);
}
