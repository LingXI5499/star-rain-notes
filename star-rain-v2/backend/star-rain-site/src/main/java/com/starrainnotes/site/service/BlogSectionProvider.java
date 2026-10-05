package com.starrainnotes.site.service;

import com.starrainnotes.blog.api.BlogPublicApi;
import com.starrainnotes.site.api.dto.SitePublicConfig;
import com.starrainnotes.site.entity.HomeSectionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BlogSectionProvider implements HomeSectionProvider {
    private final BlogPublicApi blogs;
    private final HomeSectionLimit limits;
    @Override public String sectionCode() { return "BLOG"; }
    @Override public Object load(HomeSectionEntity section, SitePublicConfig config) {
        return blogs.latestPublished(limits.of(section));
    }
}
