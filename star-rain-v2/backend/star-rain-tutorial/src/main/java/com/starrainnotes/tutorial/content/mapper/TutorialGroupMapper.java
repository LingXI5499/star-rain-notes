package com.starrainnotes.tutorial.content.mapper;

import com.starrainnotes.tutorial.content.entity.TutorialGroupEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/*
 * 教程分组的全部 SQL 都在 mapper/tutorial/TutorialGroupMapper.xml。
 * 本接口不继承 BaseMapper，每个方法在 XML 里都有唯一对应的语句。
 */
@Mapper
public interface TutorialGroupMapper {

    int insert(TutorialGroupEntity group);

    TutorialGroupEntity selectById(@Param("id") Long id);

    List<TutorialGroupEntity> listByTutorialId(@Param("tutorialId") Long tutorialId);

    /* 发布冻结快照只取 ACTIVE 分组。 */
    List<TutorialGroupEntity> listActiveByTutorialId(@Param("tutorialId") Long tutorialId);

    long countByTutorialId(@Param("tutorialId") Long tutorialId);

    int updateTitle(@Param("id") Long id, @Param("title") String title,
                    @Param("updatedAt") LocalDateTime updatedAt);

    int updateStatus(@Param("id") Long id, @Param("status") String status,
                     @Param("updatedAt") LocalDateTime updatedAt);

    int updateSortOrder(@Param("id") Long id, @Param("sortOrder") int sortOrder,
                        @Param("updatedAt") LocalDateTime updatedAt);

    int deleteById(@Param("id") Long id);
}
