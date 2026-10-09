package com.starrainnotes.tutorial.content.api;

import java.util.Optional;

public interface TutorialReferenceApi {
    boolean exists(Long tutorialId);
    Optional<PublishedTutorial> publishedTutorial(Long tutorialId);
}
