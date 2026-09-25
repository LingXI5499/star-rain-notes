package com.starrainnotes.profile.api;

import com.starrainnotes.profile.entity.Profile;
import com.starrainnotes.profile.mapper.ProfileMapper;
import org.springframework.stereotype.Component;

@Component
public class ProfilePreview implements ProfilePreviewPort {
    private final ProfileMapper mapper;

    public ProfilePreview(ProfileMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Preview preview() {
        Profile profile = mapper.selectById(1);
        if (profile == null) {
            return new Preview(null, null, null);
        }
        return new Preview(profile.getDisplayName(), profile.getHeadline(), profile.getBio());
    }
}
