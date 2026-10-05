package com.starrainnotes.site.mapper;

import com.starrainnotes.site.entity.SiteConfigEntity;

public interface SiteConfigMapper {
    SiteConfigEntity primary();

    int update(SiteConfigEntity config);
}
