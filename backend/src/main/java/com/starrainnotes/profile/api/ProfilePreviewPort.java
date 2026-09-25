package com.starrainnotes.profile.api;

public interface ProfilePreviewPort {
    record Preview(String displayName, String headline, String bio) {
    }

    Preview preview();
}
