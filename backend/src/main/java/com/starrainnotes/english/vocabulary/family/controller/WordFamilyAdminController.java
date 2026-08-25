package com.starrainnotes.english.vocabulary.family.controller;
import com.starrainnotes.english.vocabulary.family.dto.*;
import com.starrainnotes.english.vocabulary.family.service.WordFamilyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/v1/admin/english/vocabulary/families")
public class WordFamilyAdminController {
 private final WordFamilyService service;public WordFamilyAdminController(WordFamilyService service){this.service=service;}
 @GetMapping public List<WordFamilyView>list(@RequestParam(required=false)String q){return service.list(q);}
 @GetMapping("/{id}")public WordFamilyView get(@PathVariable Long id){return service.get(id);}
 @PostMapping @ResponseStatus(HttpStatus.CREATED)public WordFamilyView create(@Valid@RequestBody WordFamilyRequest r){return service.create(r);}
 @PutMapping("/{id}")public WordFamilyView update(@PathVariable Long id,@Valid@RequestBody WordFamilyRequest r){return service.update(id,r);}
 @DeleteMapping("/{id}")@ResponseStatus(HttpStatus.NO_CONTENT)public void delete(@PathVariable Long id){service.delete(id);}
 @PostMapping("/{id}/members")@ResponseStatus(HttpStatus.CREATED)public WordFamilyMemberView member(@PathVariable Long id,@Valid@RequestBody WordFamilyMemberRequest r){return service.addMember(id,r);}
 @PutMapping("/{id}/members/{memberId}")public WordFamilyMemberView member(@PathVariable Long id,@PathVariable Long memberId,@Valid@RequestBody WordFamilyMemberRequest r){return service.updateMember(id,memberId,r);}
 @DeleteMapping("/{id}/members/{memberId}")@ResponseStatus(HttpStatus.NO_CONTENT)public void deleteMember(@PathVariable Long id,@PathVariable Long memberId){service.deleteMember(id,memberId);}
 @PostMapping("/{id}/members/{memberId}/move")@ResponseStatus(HttpStatus.NO_CONTENT)public void move(@PathVariable Long id,@PathVariable Long memberId,@RequestBody java.util.Map<String,Integer>r){service.move(id,memberId,r.getOrDefault("targetIndex",0));}
}
