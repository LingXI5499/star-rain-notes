package com.starrainnotes.media.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.starrainnotes.media.entity.MediaAsset;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for media assets (used for IMAGE-type validation and
 * public URL resolution by the site module; the full media slice is TASK-009).
 */
@Mapper
public interface MediaAssetMapper extends BaseMapper<MediaAsset> {
}
