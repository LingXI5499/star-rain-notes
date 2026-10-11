package com.starrainnotes.tutorial.content.mapper;

import com.starrainnotes.tutorial.content.entity.TutorialCategoryEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/*
 * 知识体系分类的全部 SQL 都在 mapper/tutorial/TutorialCategoryMapper.xml。
 * 本接口不继承 BaseMapper，每个方法在 XML 里都有唯一对应的语句。
 */
@Mapper
public interface TutorialCategoryMapper {

    int insert(TutorialCategoryEntity category);

    TutorialCategoryEntity selectById(@Param("id") Long id);

    List<TutorialCategoryEntity> listOrdered();

    long countBySlug(@Param("slug") String slug);

    /* 改名不动 slug：公开地址保持稳定。 */
    int updateName(@Param("id") Long id, @Param("name") String name,
                   @Param("updatedAt") LocalDateTime updatedAt);

    int updateSortOrder(@Param("id") Long id, @Param("sortOrder") int sortOrder,
                        @Param("updatedAt") LocalDateTime updatedAt);

    int deleteById(@Param("id") Long id);
}
