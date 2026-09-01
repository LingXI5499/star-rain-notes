package com.starrainnotes.english.vocabulary.family.controller;
import com.starrainnotes.english.vocabulary.family.dto.WordFamilyView;
import com.starrainnotes.english.vocabulary.family.service.WordFamilyService;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/public/english/vocabulary/families")
public class WordFamilyPublicController {
 private final WordFamilyService service;public WordFamilyPublicController(WordFamilyService service){this.service=service;}
 @GetMapping("/{slug}")public WordFamilyView get(@PathVariable String slug){return service.publicGet(slug);}
}
