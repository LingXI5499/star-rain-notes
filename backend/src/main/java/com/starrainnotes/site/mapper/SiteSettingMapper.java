package com.starrainnotes.site.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.starrainnotes.site.entity.SiteSetting;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for the singleton site settings table.
 */
@Mapper
public interface SiteSettingMapper extends BaseMapper<SiteSetting> {
}
