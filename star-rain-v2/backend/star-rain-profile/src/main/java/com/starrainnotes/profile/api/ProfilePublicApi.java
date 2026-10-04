package com.starrainnotes.profile.api;

import com.starrainnotes.profile.vo.ProfileVO;

public interface ProfilePublicApi {
    ProfileVO summary();
    Long publicProfileId();
}
