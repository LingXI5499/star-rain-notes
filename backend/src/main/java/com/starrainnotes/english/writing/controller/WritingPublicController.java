package com.starrainnotes.english.writing.controller;
import com.starrainnotes.english.writing.dto.*;
import com.starrainnotes.english.writing.service.WritingPromptService;
import com.starrainnotes.english.writing.service.WritingResourceService;
import com.starrainnotes.english.writing.service.WritingExerciseService;
import com.starrainnotes.english.reading.dto.ReadingCheckAnswerRequest;
import com.starrainnotes.english.reading.dto.ReadingCheckResultView;
import com.starrainnotes.english.reading.dto.ReadingExercisePublicView;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/public/english/writing")
public class WritingPublicController {
 private final WritingResourceService resources;private final WritingPromptService prompts;private final WritingExerciseService exercises;
 public WritingPublicController(WritingResourceService resources,WritingPromptService prompts,WritingExerciseService exercises){this.resources=resources;this.prompts=prompts;this.exercises=exercises;}
 @GetMapping("/resources") public WritingPageView<WritingResourceSummaryView> resources(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int pageSize,@RequestParam(required=false)String q,@RequestParam(required=false)String kind,@RequestParam(required=false)String expressionLevel,@RequestParam(required=false)String cefr,@RequestParam(required=false)Long topic,@RequestParam(required=false)Long genre){return resources.publicList(page,pageSize,q,kind,expressionLevel,cefr,topic,genre);}
 @GetMapping("/resources/{slug}") public WritingResourceView resource(@PathVariable String slug){return resources.publicGet(slug);}
 @GetMapping("/prompts") public WritingPageView<WritingPromptSummaryView> prompts(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int pageSize,@RequestParam(required=false)String q,@RequestParam(required=false)String cefr,@RequestParam(required=false)Long topic,@RequestParam(required=false)Long genre){return prompts.publicList(page,pageSize,q,cefr,topic,genre);}
 @GetMapping("/prompts/{slug}") public WritingPromptView prompt(@PathVariable String slug){return prompts.publicGet(slug);}
 @GetMapping("/prompts/{slug}/exercises") public java.util.List<ReadingExercisePublicView> exercises(@PathVariable String slug){return exercises.publicList(slug);}
 @PostMapping("/prompts/{slug}/check") public ReadingCheckResultView check(@PathVariable String slug,@org.springframework.web.bind.annotation.RequestBody @jakarta.validation.Valid ReadingCheckAnswerRequest request){return exercises.check(slug,request);}
}
