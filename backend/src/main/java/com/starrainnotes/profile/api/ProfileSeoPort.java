package com.starrainnotes.profile.api;

import java.time.LocalDateTime;

public interface ProfileSeoPort {
    record About(String displayName, String headline, String bio, String githubUrl, String technicalDirection,
                 String journey, LocalDateTime updatedAt) {
    }

    String displayName();

    About about();
}
