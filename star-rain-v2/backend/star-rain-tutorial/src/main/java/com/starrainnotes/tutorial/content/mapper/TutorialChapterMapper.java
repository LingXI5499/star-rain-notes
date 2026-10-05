package com.starrainnotes.tutorial.content.mapper;

import com.starrainnotes.tutorial.content.entity.TutorialChapterEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/*
 * 章节工作区的全部 SQL 都在 mapper/tutorial/TutorialChapterMapper.xml。
 * 本接口不继承 BaseMapper，每个方法在 XML 里都有唯一对应的语句。
 */
@Mapper
public interface TutorialChapterMapper {

    int insert(TutorialChapterEntity chapter);

    TutorialChapterEntity selectById(@Param("id") Long id);

    List<TutorialChapterEntity> listByGroupId(@Param("groupId") Long groupId);

    /* 发布冻结快照只取 ACTIVE 章节。 */
    List<TutorialChapterEntity> listActiveByGroupId(@Param("groupId") Long groupId);

    long countByTutorialId(@Param("tutorialId") Long tutorialId);

    long countActiveByGroupId(@Param("groupId") Long groupId);

    long countByTutorialIdAndSlug(@Param("tutorialId") Long tutorialId, @Param("slug") String slug);

    /* summary 允许被清空：必须无条件写进 SET（旧 @TableField(ALWAYS) 语义）。 */
    int updateContent(@Param("id") Long id, @Param("title") String title,
                      @Param("summary") String summary, @Param("updatedAt") LocalDateTime updatedAt);

    int updateBody(@Param("id") Long id, @Param("bodyMarkdown") String bodyMarkdown,
                   @Param("updatedAt") LocalDateTime updatedAt);

    int updateStatus(@Param("id") Long id, @Param("status") String status,
                     @Param("updatedAt") LocalDateTime updatedAt);

    int updateSortOrder(@Param("id") Long id, @Param("sortOrder") int sortOrder,
                        @Param("updatedAt") LocalDateTime updatedAt);

    int moveToGroup(@Param("id") Long id, @Param("groupId") Long groupId,
                    @Param("sortOrder") int sortOrder, @Param("updatedAt") LocalDateTime updatedAt);
}
