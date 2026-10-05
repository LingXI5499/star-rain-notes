package com.starrainnotes.site.mapper;

import com.starrainnotes.site.entity.HomeSectionEntity;

import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface HomeSectionMapper {
    List<HomeSectionEntity> all();

    HomeSectionEntity byCode(@Param("code") String code);

    int update(HomeSectionEntity section);
}
