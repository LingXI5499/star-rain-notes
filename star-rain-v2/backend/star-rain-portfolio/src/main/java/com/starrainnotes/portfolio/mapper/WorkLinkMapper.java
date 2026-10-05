package com.starrainnotes.portfolio.mapper;

import com.starrainnotes.portfolio.entity.WorkLinkEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/*
 * 作品外部链接。SQL 全部在 mapper/portfolio/WorkLinkMapper.xml。
 *
 * 与媒体一样，原先一次 updateById 同时承担「改链接内容」和「只改排序」，
 * 迁移后拆成两条列范围明确的 UPDATE。
 */
public interface WorkLinkMapper {

    int insert(WorkLinkEntity link);

    WorkLinkEntity selectById(@Param("id") Long id);

    List<WorkLinkEntity> listByWorkId(@Param("workId") Long workId);

    int updateContent(WorkLinkEntity link);

    int updateSortOrder(WorkLinkEntity link);

    int deleteById(@Param("id") Long id);
}
