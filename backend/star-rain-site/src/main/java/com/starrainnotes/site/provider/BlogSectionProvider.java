package com.starrainnotes.site.provider;

import com.starrainnotes.blog.api.BlogPublicApi;
import com.starrainnotes.site.api.vo.SitePublicConfigVO;
import com.starrainnotes.site.entity.HomeSectionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BlogSectionProvider implements HomeSectionProvider {
    private final BlogPublicApi blogs;
    private final HomeSectionDisplayOptions limits;
    @Override public String sectionCode() { return "BLOG"; }
    @Override public Object load(HomeSectionEntity section, SitePublicConfigVO config) {
        return blogs.latestPublished(limits.limitOf(section));
    }
}
