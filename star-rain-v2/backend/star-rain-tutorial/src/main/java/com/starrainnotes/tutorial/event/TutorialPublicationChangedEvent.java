package com.starrainnotes.tutorial.event;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TutorialPublicationChangedEvent {
    private Long tutorialId;
    private String slug;
    private String title;
    private String action;
    private LocalDateTime publishedAt;
}
