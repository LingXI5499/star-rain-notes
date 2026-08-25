package com.starrainnotes.english.shared.learning.controller;

import com.starrainnotes.english.shared.learning.dto.*;
import com.starrainnotes.english.shared.learning.service.EnglishLearningService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/english/learning")
public class EnglishLearningPublicController {
    private final EnglishLearningService service;
    public EnglishLearningPublicController(EnglishLearningService service){this.service=service;}

    @GetMapping("/records/{contentType}/{contentId}")
    public LearningRecordView get(@RequestHeader("X-Learner-Key")String key,@PathVariable String contentType,@PathVariable Long contentId){return service.get(key,contentType,contentId);}
    @PutMapping("/records/{contentType}/{contentId}")
    public LearningRecordView save(@RequestHeader("X-Learner-Key")String key,@PathVariable String contentType,@PathVariable Long contentId,@Valid @RequestBody LearningRecordRequest request){return service.save(key,contentType,contentId,request);}
    @GetMapping("/summary")
    public LearningSummaryView summary(@RequestHeader("X-Learner-Key")String key){return service.summary(key);}
    @GetMapping("/insights")
    public LearningInsightsView insights(@RequestHeader("X-Learner-Key")String key){return service.insights(key);}
    @GetMapping("/writing-submissions/{promptId}")
    public WritingSubmissionView submission(@RequestHeader("X-Learner-Key")String key,@PathVariable Long promptId){return service.getSubmission(key,promptId);}
    @PutMapping("/writing-submissions/{promptId}")
    public WritingSubmissionView saveSubmission(@RequestHeader("X-Learner-Key")String key,@PathVariable Long promptId,@Valid @RequestBody WritingSubmissionRequest request){return service.saveSubmission(key,promptId,request);}
}
