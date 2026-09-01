package com.starrainnotes.tutorial.controller;

import com.starrainnotes.account.review.dto.ContentReviewView;
import com.starrainnotes.account.review.service.ContentReviewService;
import com.starrainnotes.account.security.AccountPrincipal;
import com.starrainnotes.tutorial.dto.AdminCurriculumView;
import com.starrainnotes.tutorial.dto.AdminTreeNodeView;
import com.starrainnotes.tutorial.dto.ChapterDetailView;
import com.starrainnotes.tutorial.dto.CreateChapterRequest;
import com.starrainnotes.tutorial.dto.CreateGroupRequest;
import com.starrainnotes.tutorial.dto.MoveIndexRequest;
import com.starrainnotes.tutorial.dto.MoveNodeRequest;
import com.starrainnotes.tutorial.dto.ReassignChapterRequest;
import com.starrainnotes.tutorial.dto.UpdateChapterRequest;
import com.starrainnotes.tutorial.dto.UpdateGroupRequest;
import com.starrainnotes.tutorial.service.TutorialNodeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin tutorial node tree, group/chapter management and move (04 §10).
 */
@RestController
@RequestMapping("/api/v1/admin/tutorials/{tutorialId}")
public class TutorialNodeAdminController {

    private final TutorialNodeService nodeService;
    private final ContentReviewService reviewService;

    public TutorialNodeAdminController(TutorialNodeService nodeService, ContentReviewService reviewService) {
        this.nodeService = nodeService;
        this.reviewService = reviewService;
    }

    @GetMapping("/nodes")
    public List<AdminTreeNodeView> tree(@PathVariable Long tutorialId) {
        return nodeService.tree(tutorialId);
    }

    @GetMapping("/curriculum")
    public AdminCurriculumView curriculum(@PathVariable Long tutorialId) {
        return nodeService.curriculum(tutorialId);
    }

    // ---------------------------------------------------------------
    // groups
    // ---------------------------------------------------------------

    @PostMapping("/groups")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminTreeNodeView createGroup(@PathVariable Long tutorialId,
                                         @Valid @RequestBody CreateGroupRequest request) {
        return nodeService.createGroup(tutorialId, request);
    }

    @PutMapping("/groups/{groupId}")
    public AdminTreeNodeView updateGroup(@PathVariable Long tutorialId,
                                         @PathVariable Long groupId,
                                         @Valid @RequestBody UpdateGroupRequest request) {
        return nodeService.updateGroup(tutorialId, groupId, request);
    }

    @DeleteMapping("/groups/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGroup(@PathVariable Long tutorialId, @PathVariable Long groupId) {
        nodeService.deleteGroup(tutorialId, groupId);
    }

    @PostMapping("/groups/{groupId}/move")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void moveGroup(@PathVariable Long tutorialId,
                          @PathVariable Long groupId,
                          @Valid @RequestBody MoveIndexRequest request) {
        nodeService.moveGroup(tutorialId, groupId, request);
    }

    // ---------------------------------------------------------------
    // chapters
    // ---------------------------------------------------------------

    @PostMapping("/chapters")
    @ResponseStatus(HttpStatus.CREATED)
    public ChapterDetailView createChapter(@PathVariable Long tutorialId,
                                           @Valid @RequestBody CreateChapterRequest request) {
        return nodeService.createChapter(tutorialId, request);
    }

    @GetMapping("/chapters/{chapterId}")
    public ChapterDetailView getChapter(@PathVariable Long tutorialId, @PathVariable Long chapterId) {
        return nodeService.getChapter(tutorialId, chapterId);
    }

    @PutMapping("/chapters/{chapterId}")
    public ResponseEntity<?> updateChapter(@PathVariable Long tutorialId,
                                           @PathVariable Long chapterId,
                                           @Valid @RequestBody UpdateChapterRequest request,
                                           Authentication authentication) {
        if (!isSuperAdmin(authentication)
                && "PUBLISHED".equals(nodeService.chapterPublishStatus(tutorialId, chapterId))) {
            ContentReviewView review = reviewService.submitTutorialChapterUpdate(
                    actorId(authentication), tutorialId, chapterId, request);
            return ResponseEntity.accepted().body(review);
        }
        return ResponseEntity.ok(nodeService.updateChapter(tutorialId, chapterId, request));
    }

    @DeleteMapping("/chapters/{chapterId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteChapter(@PathVariable Long tutorialId, @PathVariable Long chapterId) {
        nodeService.deleteChapter(tutorialId, chapterId);
    }

    @PostMapping("/chapters/{chapterId}/publish")
    public ChapterDetailView publishChapter(@PathVariable Long tutorialId, @PathVariable Long chapterId) {
        return nodeService.publishChapter(tutorialId, chapterId);
    }

    @PostMapping("/chapters/{chapterId}/withdraw")
    public ChapterDetailView withdrawChapter(@PathVariable Long tutorialId, @PathVariable Long chapterId) {
        return nodeService.withdrawChapter(tutorialId, chapterId);
    }

    @PostMapping("/chapters/{chapterId}/move")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void moveChapter(@PathVariable Long tutorialId,
                            @PathVariable Long chapterId,
                            @Valid @RequestBody MoveIndexRequest request) {
        nodeService.moveChapter(tutorialId, chapterId, request);
    }

    @PostMapping("/chapters/{chapterId}/reassign")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reassignChapter(@PathVariable Long tutorialId,
                                @PathVariable Long chapterId,
                                @Valid @RequestBody ReassignChapterRequest request) {
        nodeService.reassignChapter(tutorialId, chapterId, request);
    }

    // ---------------------------------------------------------------
    // move
    // ---------------------------------------------------------------

    @PostMapping("/nodes/{nodeId}/move")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void moveNode(@PathVariable Long tutorialId,
                         @PathVariable Long nodeId,
                         @Valid @RequestBody MoveNodeRequest request) {
        nodeService.moveNode(tutorialId, nodeId, request);
    }

    private boolean isSuperAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_SUPER_ADMIN".equals(authority.getAuthority()));
    }

    private Long actorId(Authentication authentication) {
        Object principal = authentication == null ? null : authentication.getPrincipal();
        return principal instanceof AccountPrincipal account ? account.getId() : null;
    }
}
