package com.starrainnotes.portfolio.mapper;

import com.starrainnotes.portfolio.entity.WorkMediaEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/*
 * 作品媒体编排。SQL 全部在 mapper/portfolio/WorkMediaMapper.xml。
 *
 * 原先一次 updateById 既承担「改媒体用途与说明」，也承担「只改排序」，
 * 两条路径改动的列完全不同，因此拆成 updateContent 与 updateSortOrder，
 * 让「改排序不会顺手覆盖 caption / usage_type」成为 XML 里看得见的事实。
 */
public interface WorkMediaMapper {

    int insert(WorkMediaEntity media);

    WorkMediaEntity selectById(@Param("id") Long id);

    List<WorkMediaEntity> listByWorkId(@Param("workId") Long workId);

    int updateContent(WorkMediaEntity media);

    int updateSortOrder(WorkMediaEntity media);

    int deleteById(@Param("id") Long id);
}
