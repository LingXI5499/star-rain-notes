package com.starrainnotes.tutorial.content.api;

import java.util.List;

public interface TutorialPublicApi {
    List<PublishedTutorial> latestPublished(int limit);
}
