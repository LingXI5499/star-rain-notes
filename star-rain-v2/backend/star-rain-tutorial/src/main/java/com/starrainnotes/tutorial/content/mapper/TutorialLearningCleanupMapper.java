package com.starrainnotes.tutorial.content.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Retires affected plans while preserving immutable personal learning history. */
@Mapper
public interface TutorialLearningCleanupMapper {
    int cancelPlans(@Param("scope") String scope, @Param("id") Long id);
    int skipCancelledTasks(@Param("scope") String scope, @Param("id") Long id);
    int deleteChapterRelations(@Param("chapterId") Long chapterId);
}
