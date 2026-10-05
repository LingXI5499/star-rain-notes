package com.starrainnotes.profile.api;

import com.starrainnotes.profile.api.dto.ProfileVO;
import com.starrainnotes.profile.api.dto.ProfilePublishedDocument;

public interface ProfilePublicApi {
    ProfileVO summary();
    Long publicProfileId();
    ProfilePublishedDocument publishedDocument();
}
