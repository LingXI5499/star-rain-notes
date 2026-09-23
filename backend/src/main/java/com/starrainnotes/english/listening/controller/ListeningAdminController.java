package com.starrainnotes.english.listening.controller;

import com.starrainnotes.account.review.dto.ContentReviewView;
import com.starrainnotes.account.review.service.ContentReviewService;
import com.starrainnotes.account.security.AccountPrincipal;
import com.starrainnotes.english.listening.dto.ListeningItemRequest;
import com.starrainnotes.english.listening.dto.ListeningItemView;
import com.starrainnotes.english.listening.dto.ListeningMoveRequest;
import com.starrainnotes.english.listening.dto.ListeningPageView;
import com.starrainnotes.english.listening.dto.ListeningSegmentBatchRequest;
import com.starrainnotes.english.listening.dto.ListeningSegmentRequest;
import com.starrainnotes.english.listening.dto.ListeningSegmentView;
import com.starrainnotes.english.listening.dto.PronunciationRuleRequest;
import com.starrainnotes.english.listening.dto.PronunciationRuleView;
import com.starrainnotes.english.listening.dto.ReadingPairRef;
import com.starrainnotes.english.listening.dto.ReadingPairRequest;
import com.starrainnotes.english.listening.application.ListeningQueryService;
import com.starrainnotes.english.listening.application.ListeningCommandService;
import com.starrainnotes.english.listening.application.ListeningSegmentService;
import com.starrainnotes.english.listening.application.ListeningRelationService;
import com.starrainnotes.english.listening.application.PronunciationRuleQueryService;
import com.starrainnotes.english.listening.application.PronunciationRuleCommandService;
import com.starrainnotes.english.listening.application.ListeningExerciseApplicationService;
import com.starrainnotes.english.shared.exercise.dto.CheckAnswerRequest;
import com.starrainnotes.english.shared.exercise.dto.CheckResultView;
import com.starrainnotes.english.shared.exercise.dto.ExerciseMoveRequest;
import com.starrainnotes.english.shared.exercise.dto.ExercisePublicView;
import com.starrainnotes.english.shared.exercise.dto.ExerciseRequest;
import com.starrainnotes.english.shared.exercise.dto.ExerciseView;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin listening management (阶段三 §四). Writes use the frozen session + CSRF.
 */
@RestController
@RequestMapping("/api/v1/admin/english/listening")
public class ListeningAdminController {

    private final ListeningQueryService queries;
    private final ListeningCommandService commands;
    private final ListeningSegmentService segments;
    private final ListeningRelationService relations;
    private final PronunciationRuleQueryService pronunciationQueries;
    private final PronunciationRuleCommandService pronunciationCommands;
    private final ListeningExerciseApplicationService exerciseService;
    private final ContentReviewService reviewService;

    public ListeningAdminController(ListeningQueryService queries, ListeningCommandService commands,
            ListeningSegmentService segments, ListeningRelationService relations,
            PronunciationRuleQueryService pronunciationQueries,
            PronunciationRuleCommandService pronunciationCommands,
            ListeningExerciseApplicationService exerciseService, ContentReviewService reviewService) {
        this.queries = queries;
        this.commands = commands;
        this.segments = segments;
        this.relations = relations;
        this.pronunciationQueries = pronunciationQueries;
        this.pronunciationCommands = pronunciationCommands;
        this.exerciseService = exerciseService;
        this.reviewService = reviewService;
    }

    @GetMapping("/items")
    public ListeningPageView list(@RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "20") int pageSize,
                                  @RequestParam(required = false) String q,
                                  @RequestParam(required = false) String status,
                                  @RequestParam(required = false) Integer level,
                                  @RequestParam(required = false) String cefr,
                                  @RequestParam(required = false) Long topic,
                                  @RequestParam(required = false) Long scene,
                                  @RequestParam(required = false) Long format) {
        return queries.list(page, pageSize, q, status, level, cefr, topic, scene, format);
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public ListeningItemView create(@Valid @RequestBody ListeningItemRequest request) {
        return commands.create(request);
    }

    @GetMapping("/items/{id}")
    public ListeningItemView get(@PathVariable Long id) {
        return queries.get(id);
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody ListeningItemRequest request,
                                    Authentication authentication) {
        if (!isSuperAdmin(authentication) && reviewService.isPublished("ENGLISH_LISTENING_ITEM", id)) {
            ContentReviewView review = reviewService.submitEnglishUpdate(actorId(authentication),
                    "ENGLISH_LISTENING_ITEM", id, request.title(), request);
            return ResponseEntity.accepted().body(review);
        }
        return ResponseEntity.ok(commands.update(id, request));
    }

    @DeleteMapping("/items/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        commands.delete(id);
    }

    @PostMapping("/items/{id}/publish")
    public ListeningItemView publish(@PathVariable Long id) {
        return commands.publish(id);
    }

    @PostMapping("/items/{id}/withdraw")
    public ListeningItemView withdraw(@PathVariable Long id) {
        return commands.withdraw(id);
    }

    // segments
    @GetMapping("/items/{id}/segments")
    public List<ListeningSegmentView> segments(@PathVariable Long id) {
        return segments.list(id);
    }

    @PostMapping("/items/{id}/segments")
    @ResponseStatus(HttpStatus.CREATED)
    public ListeningSegmentView createSegment(@PathVariable Long id, @Valid @RequestBody ListeningSegmentRequest request) {
        return segments.create(id, request);
    }

    @PutMapping("/items/{id}/segments/{segmentId}")
    public ListeningSegmentView updateSegment(@PathVariable Long id, @PathVariable Long segmentId,
                                              @Valid @RequestBody ListeningSegmentRequest request) {
        return segments.update(id, segmentId, request);
    }

    @DeleteMapping("/items/{id}/segments/{segmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSegment(@PathVariable Long id, @PathVariable Long segmentId) {
        segments.delete(id, segmentId);
    }

    @PostMapping("/items/{id}/segments/{segmentId}/move")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void moveSegment(@PathVariable Long id, @PathVariable Long segmentId,
                            @RequestBody ListeningMoveRequest request) {
        segments.move(id, segmentId,
                request.targetIndex() == null ? 0 : request.targetIndex());
    }

    @PutMapping("/items/{id}/segments/batch")
    public List<ListeningSegmentView> batchSegments(@PathVariable Long id,
                                                    @RequestBody ListeningSegmentBatchRequest request) {
        return segments.replaceBatch(id, request.segments());
    }

    // exercises
    @GetMapping("/items/{id}/exercises")
    public List<ExerciseView> exercises(@PathVariable Long id) {
        return exerciseService.listByItem(id);
    }

    @PostMapping("/items/{id}/exercises")
    @ResponseStatus(HttpStatus.CREATED)
    public ExerciseView createExercise(@PathVariable Long id, @Valid @RequestBody ExerciseRequest request) {
        return exerciseService.create(id, request);
    }

    @PutMapping("/items/{id}/exercises/{exerciseId}")
    public ExerciseView updateExercise(@PathVariable Long id, @PathVariable Long exerciseId,
                                              @Valid @RequestBody ExerciseRequest request) {
        return exerciseService.update(id, exerciseId, request);
    }

    @DeleteMapping("/items/{id}/exercises/{exerciseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExercise(@PathVariable Long id, @PathVariable Long exerciseId) {
        exerciseService.delete(id, exerciseId);
    }

    @PostMapping("/items/{id}/exercises/{exerciseId}/move")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void moveExercise(@PathVariable Long id, @PathVariable Long exerciseId,
                             @RequestBody ExerciseMoveRequest request) {
        exerciseService.move(id, exerciseId, request.targetIndex() == null ? 0 : request.targetIndex());
    }

    // reading pairs
    @GetMapping("/items/{id}/reading-pairs")
    public List<ReadingPairRef> readingPairs(@PathVariable Long id) {
        return relations.readingPairs(id);
    }

    @PostMapping("/items/{id}/reading-pairs")
    @ResponseStatus(HttpStatus.CREATED)
    public void addReadingPair(@PathVariable Long id, @Valid @RequestBody ReadingPairRequest request) {
        relations.addReadingPair(id, request);
    }

    @DeleteMapping("/items/{id}/reading-pairs/{readingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeReadingPair(@PathVariable Long id, @PathVariable Long readingId) {
        relations.removeReadingPair(id, readingId);
    }

    // pronunciation rules
    @GetMapping("/pronunciation")
    public List<PronunciationRuleView> rules() {
        return pronunciationQueries.list(false);
    }

    @PostMapping("/pronunciation")
    @ResponseStatus(HttpStatus.CREATED)
    public PronunciationRuleView createRule(@Valid @RequestBody PronunciationRuleRequest request) {
        return pronunciationCommands.create(request);
    }

    @GetMapping("/pronunciation/{id}")
    public PronunciationRuleView rule(@PathVariable Long id) {
        return pronunciationQueries.get(id);
    }

    @PutMapping("/pronunciation/{id}")
    public ResponseEntity<?> updateRule(@PathVariable Long id, @Valid @RequestBody PronunciationRuleRequest request,
                                        Authentication authentication) {
        if (!isSuperAdmin(authentication) && reviewService.isPublished("ENGLISH_PRONUNCIATION_RULE", id)) {
            ContentReviewView review = reviewService.submitEnglishUpdate(actorId(authentication),
                    "ENGLISH_PRONUNCIATION_RULE", id, request.title(), request);
            return ResponseEntity.accepted().body(review);
        }
        return ResponseEntity.ok(pronunciationCommands.update(id, request));
    }

    @DeleteMapping("/pronunciation/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRule(@PathVariable Long id) {
        pronunciationCommands.delete(id);
    }

    @PostMapping("/pronunciation/{id}/publish")
    public PronunciationRuleView publishRule(@PathVariable Long id) {
        return pronunciationCommands.publish(id);
    }

    @PostMapping("/pronunciation/{id}/withdraw")
    public PronunciationRuleView withdrawRule(@PathVariable Long id) {
        return pronunciationCommands.withdraw(id);
    }

    @PostMapping("/pronunciation/{id}/move")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void moveRule(@PathVariable Long id, @RequestBody ListeningMoveRequest request) {
        pronunciationCommands.move(id, request.targetIndex() == null ? 0 : request.targetIndex());
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
