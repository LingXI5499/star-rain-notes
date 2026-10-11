package com.starrainnotes.english.writing.article.service;
import com.starrainnotes.english.writing.article.dto.WritingArticleDto.*;
import com.starrainnotes.english.knowledge.dto.RevisionDto;
public interface WritingArticleService {
    Page list(boolean mine,String search,String state,Long topic,Long genre,Long purpose,int page,int size);
    Article get(String id); Article getPublic(String slug); Article create(Request request); Article save(String id,Request request);
    Article complete(String id,Long version); Article publish(String id,Long version,boolean publish); void delete(String id);
    RevisionDto.Page revisions(String id,int page,int size); RevisionDto.Revision revision(String id,long no);
    RevisionDto.Revision snapshot(String id,RevisionDto.Request request); Article restore(String id,long no,RevisionDto.Request request);
}
