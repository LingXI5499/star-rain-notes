package com.starrainnotes.tutorial.learning.mapper;

import com.starrainnotes.tutorial.learning.entity.StudyPlanEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/*
 * 个人学习计划的全部 SQL 都在 mapper/tutorial/StudyPlanMapper.xml。
 * 本接口不继承 BaseMapper，每个方法在 XML 里都有唯一对应的语句。
 */
@Mapper
public interface StudyPlanMapper {

    int insert(StudyPlanEntity plan);

    StudyPlanEntity selectById(@Param("id") Long id);

    List<StudyPlanEntity> listRecentByAccountId(@Param("accountId") Long accountId);

    long countActiveByAccountId(@Param("accountId") Long accountId);

    /*
     * 保存计划定义。end_date / daily_target_minutes 允许为空，空值不覆盖已有值 ——
     * 这与原先 updateById 的 NOT_NULL 字段策略一致，改成一个无条件 SET 会让「不传」变成「清空」。
     */
    int updateDefinition(StudyPlanEntity plan);

    int updateStatus(@Param("id") Long id, @Param("status") String status,
                     @Param("generatedVersion") Integer generatedVersion);
}
