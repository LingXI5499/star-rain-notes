package com.starrainnotes.english.vocabulary.family.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record WordFamilyMemberRequest(@NotBlank @Size(max=200)String spelling,@Size(max=50)String partOfSpeech,@Size(max=100)String phoneticUs,@Size(max=1000)String translation,@Size(max=2)String cefrLevel,@Size(max=1000)String exampleSentence,@Size(max=1000)String exampleTranslation,Integer sortOrder){}
