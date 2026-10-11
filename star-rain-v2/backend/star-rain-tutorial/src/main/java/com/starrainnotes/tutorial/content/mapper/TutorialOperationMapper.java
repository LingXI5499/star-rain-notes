package com.starrainnotes.tutorial.content.mapper;

import com.starrainnotes.tutorial.content.entity.TutorialOperationEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface TutorialOperationMapper {
    int recordFailure(@org.apache.ibatis.annotations.Param("id") Long id, @org.apache.ibatis.annotations.Param("message") String message);
    List<Long> lockCatalog();
    int insert(TutorialOperationEntity row);
    TutorialOperationEntity byIdForUpdate(@Param("id") Long id);
    TutorialOperationEntity selectById(@Param("id") Long id);
    TutorialOperationEntity pending(@Param("scope") String scope);
    List<TutorialOperationEntity> pendingOperations();
    int finish(@Param("id") Long id, @Param("status") String status);
}
