package com.starrainnotes.english.writing.controller;
import com.starrainnotes.english.writing.dto.*;
import com.starrainnotes.english.writing.service.WritingPromptService;
import com.starrainnotes.english.writing.service.WritingResourceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/admin/english/writing")
public class WritingAdminController {
 private final WritingResourceService resources; private final WritingPromptService prompts;
 public WritingAdminController(WritingResourceService resources,WritingPromptService prompts){this.resources=resources;this.prompts=prompts;}
 @GetMapping("/resources") public WritingPageView<WritingResourceSummaryView> resources(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int pageSize,@RequestParam(required=false)String q,@RequestParam(required=false)String kind,@RequestParam(required=false)String expressionLevel,@RequestParam(required=false)String cefr,@RequestParam(required=false)String status,@RequestParam(required=false)Long topic,@RequestParam(required=false)Long genre){return resources.list(page,pageSize,q,kind,expressionLevel,cefr,status,topic,genre);}
 @PostMapping("/resources") @ResponseStatus(HttpStatus.CREATED) public WritingResourceView createResource(@Valid @RequestBody WritingResourceRequest r){return resources.create(r);}
 @GetMapping("/resources/{id}") public WritingResourceView resource(@PathVariable Long id){return resources.get(id);}
 @PutMapping("/resources/{id}") public WritingResourceView updateResource(@PathVariable Long id,@Valid @RequestBody WritingResourceRequest r){return resources.update(id,r);}
 @DeleteMapping("/resources/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteResource(@PathVariable Long id){resources.delete(id);}
 @PostMapping("/resources/{id}/publish") public WritingResourceView publishResource(@PathVariable Long id){return resources.publish(id);}
 @PostMapping("/resources/{id}/withdraw") public WritingResourceView withdrawResource(@PathVariable Long id){return resources.withdraw(id);}
 @PostMapping("/resources/{id}/move") @ResponseStatus(HttpStatus.NO_CONTENT) public void moveResource(@PathVariable Long id,@RequestBody WritingMoveRequest r){resources.move(id,r.targetIndex()==null?0:r.targetIndex());}
 @GetMapping("/prompts") public WritingPageView<WritingPromptSummaryView> prompts(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int pageSize,@RequestParam(required=false)String q,@RequestParam(required=false)String cefr,@RequestParam(required=false)String status,@RequestParam(required=false)Long topic,@RequestParam(required=false)Long genre){return prompts.list(page,pageSize,q,cefr,status,topic,genre);}
 @PostMapping("/prompts") @ResponseStatus(HttpStatus.CREATED) public WritingPromptView createPrompt(@Valid @RequestBody WritingPromptRequest r){return prompts.create(r);}
 @GetMapping("/prompts/{id}") public WritingPromptView prompt(@PathVariable Long id){return prompts.get(id);}
 @PutMapping("/prompts/{id}") public WritingPromptView updatePrompt(@PathVariable Long id,@Valid @RequestBody WritingPromptRequest r){return prompts.update(id,r);}
 @DeleteMapping("/prompts/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deletePrompt(@PathVariable Long id){prompts.delete(id);}
 @PostMapping("/prompts/{id}/publish") public WritingPromptView publishPrompt(@PathVariable Long id){return prompts.publish(id);}
 @PostMapping("/prompts/{id}/withdraw") public WritingPromptView withdrawPrompt(@PathVariable Long id){return prompts.withdraw(id);}
 @PostMapping("/prompts/{id}/move") @ResponseStatus(HttpStatus.NO_CONTENT) public void movePrompt(@PathVariable Long id,@RequestBody WritingMoveRequest r){prompts.move(id,r.targetIndex()==null?0:r.targetIndex());}
}
