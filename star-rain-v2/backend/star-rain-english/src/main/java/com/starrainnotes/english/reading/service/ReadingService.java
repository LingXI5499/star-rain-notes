package com.starrainnotes.english.reading.service;
import com.starrainnotes.english.reading.dto.ReadingDto.*;
public interface ReadingService {
    Page list(boolean admin,String search,int page,int size);
    Page listFiltered(boolean admin,String search,int page,int size,Long topicId,Long genreId,Long purposeId);
    Article get(String identity,boolean admin);
    Article create(Request request);
    Article update(String identity,Request request);
    Article restoreSnapshot(String identity,Request request);
    Article setPublished(String identity,boolean published);
    Article setPublished(String identity,boolean published,Long expectedVersion);
    void delete(String identity);
}
