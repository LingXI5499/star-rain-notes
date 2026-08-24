package com.starrainnotes.english.listening.controller;

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
import com.starrainnotes.english.listening.service.ListeningExerciseService;
import com.starrainnotes.english.listening.service.ListeningItemService;
import com.starrainnotes.english.reading.dto.ReadingCheckAnswerRequest;
import com.starrainnotes.english.reading.dto.ReadingCheckResultView;
import com.starrainnotes.english.reading.dto.ReadingExerciseMoveRequest;
import com.starrainnotes.english.reading.dto.ReadingExercisePublicView;
import com.starrainnotes.english.reading.dto.ReadingExerciseRequest;
import com.starrainnotes.english.reading.dto.ReadingExerciseView;
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

import java.util.List;

/**
 * Admin listening management (阶段三 §四). Writes use the frozen session + CSRF.
 */
@RestController
@RequestMapping("/api/v1/admin/english/listening")
public class ListeningAdminController {

    private final ListeningItemService itemService;
    private final ListeningExerciseService exerciseService;

    public ListeningAdminController(ListeningItemService itemService, ListeningExerciseService exerciseService) {
        this.itemService = itemService;
        this.exerciseService = exerciseService;
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
        return itemService.list(page, pageSize, q, status, level, cefr, topic, scene, format);
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public ListeningItemView create(@Valid @RequestBody ListeningItemRequest request) {
        return itemService.create(request);
    }

    @GetMapping("/items/{id}")
    public ListeningItemView get(@PathVariable Long id) {
        return itemService.get(id);
    }

    @PutMapping("/items/{id}")
    public ListeningItemView update(@PathVariable Long id, @Valid @RequestBody ListeningItemRequest request) {
        return itemService.update(id, request);
    }

    @DeleteMapping("/items/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        itemService.delete(id);
    }

    @PostMapping("/items/{id}/publish")
    public ListeningItemView publish(@PathVariable Long id) {
        return itemService.publish(id);
    }

    @PostMapping("/items/{id}/withdraw")
    public ListeningItemView withdraw(@PathVariable Long id) {
        return itemService.withdraw(id);
    }

    // segments
    @GetMapping("/items/{id}/segments")
    public List<ListeningSegmentView> segments(@PathVariable Long id) {
        return itemService.segments(id);
    }

    @PostMapping("/items/{id}/segments")
    @ResponseStatus(HttpStatus.CREATED)
    public ListeningSegmentView createSegment(@PathVariable Long id, @Valid @RequestBody ListeningSegmentRequest request) {
        return itemService.createSegment(id, request);
    }

    @PutMapping("/items/{id}/segments/{segmentId}")
    public ListeningSegmentView updateSegment(@PathVariable Long id, @PathVariable Long segmentId,
                                              @Valid @RequestBody ListeningSegmentRequest request) {
        return itemService.updateSegment(id, segmentId, request);
    }

    @DeleteMapping("/items/{id}/segments/{segmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSegment(@PathVariable Long id, @PathVariable Long segmentId) {
        itemService.deleteSegment(id, segmentId);
    }

    @PostMapping("/items/{id}/segments/{segmentId}/move")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void moveSegment(@PathVariable Long id, @PathVariable Long segmentId,
                            @RequestBody ListeningMoveRequest request) {
        itemService.moveSegment(id, segmentId,
                request.targetIndex() == null ? 0 : request.targetIndex());
    }

    @PutMapping("/items/{id}/segments/batch")
    public List<ListeningSegmentView> batchSegments(@PathVariable Long id,
                                                    @RequestBody ListeningSegmentBatchRequest request) {
        return itemService.batchSegments(id, request.segments());
    }

    // exercises
    @GetMapping("/items/{id}/exercises")
    public List<ReadingExerciseView> exercises(@PathVariable Long id) {
        return exerciseService.listByItem(id);
    }

    @PostMapping("/items/{id}/exercises")
    @ResponseStatus(HttpStatus.CREATED)
    public ReadingExerciseView createExercise(@PathVariable Long id, @Valid @RequestBody ReadingExerciseRequest request) {
        return exerciseService.create(id, request);
    }

    @PutMapping("/items/{id}/exercises/{exerciseId}")
    public ReadingExerciseView updateExercise(@PathVariable Long id, @PathVariable Long exerciseId,
                                              @Valid @RequestBody ReadingExerciseRequest request) {
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
                             @RequestBody ReadingExerciseMoveRequest request) {
        exerciseService.move(id, exerciseId, request.targetIndex() == null ? 0 : request.targetIndex());
    }

    // reading pairs
    @GetMapping("/items/{id}/reading-pairs")
    public List<ReadingPairRef> readingPairs(@PathVariable Long id) {
        return itemService.readingPairs(id);
    }

    @PostMapping("/items/{id}/reading-pairs")
    @ResponseStatus(HttpStatus.CREATED)
    public void addReadingPair(@PathVariable Long id, @Valid @RequestBody ReadingPairRequest request) {
        itemService.addReadingPair(id, request);
    }

    @DeleteMapping("/items/{id}/reading-pairs/{readingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeReadingPair(@PathVariable Long id, @PathVariable Long readingId) {
        itemService.removeReadingPair(id, readingId);
    }

    // pronunciation rules
    @GetMapping("/pronunciation")
    public List<PronunciationRuleView> rules() {
        return itemService.pronunciationRules(false);
    }

    @PostMapping("/pronunciation")
    @ResponseStatus(HttpStatus.CREATED)
    public PronunciationRuleView createRule(@Valid @RequestBody PronunciationRuleRequest request) {
        return itemService.createRule(request);
    }

    @GetMapping("/pronunciation/{id}")
    public PronunciationRuleView rule(@PathVariable Long id) {
        return itemService.ruleById(id, false);
    }

    @PutMapping("/pronunciation/{id}")
    public PronunciationRuleView updateRule(@PathVariable Long id, @Valid @RequestBody PronunciationRuleRequest request) {
        return itemService.updateRule(id, request);
    }

    @DeleteMapping("/pronunciation/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRule(@PathVariable Long id) {
        itemService.deleteRule(id);
    }

    @PostMapping("/pronunciation/{id}/publish")
    public PronunciationRuleView publishRule(@PathVariable Long id) {
        return itemService.publishRule(id);
    }

    @PostMapping("/pronunciation/{id}/withdraw")
    public PronunciationRuleView withdrawRule(@PathVariable Long id) {
        return itemService.withdrawRule(id);
    }

    @PostMapping("/pronunciation/{id}/move")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void moveRule(@PathVariable Long id, @RequestBody ListeningMoveRequest request) {
        itemService.moveRule(id, request.targetIndex() == null ? 0 : request.targetIndex());
    }
}
