package com.starrainnotes.english.reading.service;
import com.starrainnotes.english.knowledge.dto.RevisionDto.*;
import com.starrainnotes.english.reading.dto.ReadingDto.Article;
public interface ReadingRevisionService {
    Page list(String id,int page,int size); Revision get(String id,long no);
    Revision snapshot(String id,Request request); Article restore(String id,long no,Request request);
}
