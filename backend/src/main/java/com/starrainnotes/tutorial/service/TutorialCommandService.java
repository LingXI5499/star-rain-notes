package com.starrainnotes.tutorial.service;

import com.starrainnotes.tutorial.dto.AdminTutorialDetailView;
import com.starrainnotes.tutorial.dto.CreateTutorialRequest;
import com.starrainnotes.tutorial.dto.MoveTutorialRequest;
import com.starrainnotes.tutorial.dto.UpdateTutorialRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Write-side boundary for tutorial metadata, ordering and lifecycle actions. */
@Service
@RequiredArgsConstructor
public class TutorialCommandService {
    private final TutorialService tutorialService;

    public AdminTutorialDetailView create(CreateTutorialRequest request) { return tutorialService.create(request); }
    public AdminTutorialDetailView update(Long tutorialId, UpdateTutorialRequest request) { return tutorialService.update(tutorialId, request); }
    public void delete(Long tutorialId) { tutorialService.delete(tutorialId); }
    public void move(Long tutorialId, MoveTutorialRequest request) { tutorialService.move(tutorialId, request); }
    public AdminTutorialDetailView publish(Long tutorialId) { return tutorialService.publish(tutorialId); }
    public AdminTutorialDetailView withdraw(Long tutorialId) { return tutorialService.withdraw(tutorialId); }
}
