package com.starrainnotes.english.vocabulary.family.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
public record WordFamilyRequest(@NotBlank @Size(max=200)String headWord,@Size(max=150)String slug,@Size(max=1000)String description,List<Long> wordIds){}
