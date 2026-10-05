package com.starrainnotes.site.service;

import com.starrainnotes.profile.api.ProfilePublicApi;
import com.starrainnotes.site.api.dto.SitePublicConfig;
import com.starrainnotes.site.entity.HomeSectionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProfileSectionProvider implements HomeSectionProvider {
    private final ProfilePublicApi profiles;
    @Override public String sectionCode() { return "PROFILE"; }
    @Override public Object load(HomeSectionEntity section, SitePublicConfig config) {
        return profiles.summary();
    }
}
