package com.starrainnotes.portfolio.mapper;

import com.starrainnotes.portfolio.entity.WorkDetailEntity;
import org.apache.ibatis.annotations.Param;

/*
 * 作品类型扩展信息（每个作品最多一行）。SQL 全部在 mapper/portfolio/WorkDetailMapper.xml。
 *
 * 这张表只有「按 work_id 读一行」「新增」「改 detail_json」「删除」四个访问形状，
 * 逐个显式声明比让 BaseMapper 生成整套 CRUD 更能说明它只按作品维度使用。
 */
public interface WorkDetailMapper {

    int insert(WorkDetailEntity detail);

    WorkDetailEntity selectByWorkId(@Param("workId") Long workId);

    int updateDetailJson(WorkDetailEntity detail);

    int deleteById(@Param("id") Long id);
}
