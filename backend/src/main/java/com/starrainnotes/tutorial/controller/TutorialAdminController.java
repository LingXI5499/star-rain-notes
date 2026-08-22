package com.starrainnotes.tutorial.controller;

import com.starrainnotes.tutorial.dto.AdminTutorialDetailView;
import com.starrainnotes.tutorial.dto.CreateTutorialRequest;
import com.starrainnotes.tutorial.dto.TutorialPageView;
import com.starrainnotes.tutorial.dto.UpdateTutorialRequest;
import com.starrainnotes.tutorial.service.TutorialService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin tutorial management (04 §10). Publish lifecycle only via
 * /publish and /withdraw action endpoints.
 */
@RestController
@RequestMapping("/api/v1/admin/tutorials")
public class TutorialAdminController {

    private final TutorialService tutorialService;

    public TutorialAdminController(TutorialService tutorialService) {
        this.tutorialService = tutorialService;
    }

    @GetMapping
    public TutorialPageView list(@RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "10") int pageSize,
                                 @RequestParam(required = false) String status,
                                 @RequestParam(required = false) String q) {
        return tutorialService.adminList(page, pageSize, status, q);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminTutorialDetailView create(@Valid @RequestBody CreateTutorialRequest request) {
        return tutorialService.create(request);
    }

    @GetMapping("/{tutorialId}")
    public AdminTutorialDetailView detail(@PathVariable Long tutorialId) {
        return tutorialService.adminDetail(tutorialId);
    }

    @PutMapping("/{tutorialId}")
    public AdminTutorialDetailView update(@PathVariable Long tutorialId,
                                          @Valid @RequestBody UpdateTutorialRequest request) {
        return tutorialService.update(tutorialId, request);
    }

    @DeleteMapping("/{tutorialId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long tutorialId) {
        tutorialService.delete(tutorialId);
    }

    @PostMapping("/{tutorialId}/publish")
    public AdminTutorialDetailView publish(@PathVariable Long tutorialId) {
        return tutorialService.publish(tutorialId);
    }

    @PostMapping("/{tutorialId}/withdraw")
    public AdminTutorialDetailView withdraw(@PathVariable Long tutorialId) {
        return tutorialService.withdraw(tutorialId);
    }
}
