package com.starrainnotes.site.provider;

import com.starrainnotes.site.api.vo.SitePublicConfigVO;
import com.starrainnotes.site.entity.HomeSectionEntity;

public interface HomeSectionProvider {
    String sectionCode();
    Object load(HomeSectionEntity section, SitePublicConfigVO config);
}
