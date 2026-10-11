package com.starrainnotes.english.knowledge.service;
import com.starrainnotes.english.knowledge.dto.KnowledgeDto.Page;
public interface KnowledgeService { Page list(String type,String search,Long topic,Long genre,Long purpose,int page,int size); }
