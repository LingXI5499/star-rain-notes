package com.starrainnotes.english.vocabulary.family.dto;
import java.util.List;
public record WordFamilyView(Long id,String headWord,String slug,String description,List<Long>wordIds,List<WordFamilyMemberView>members,String updatedAt){}
