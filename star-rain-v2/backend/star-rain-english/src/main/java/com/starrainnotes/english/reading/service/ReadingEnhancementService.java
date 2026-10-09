package com.starrainnotes.english.reading.service;
import com.starrainnotes.english.reading.dto.ReadingEnhancementDto.*;
import java.util.List;
public interface ReadingEnhancementService {
    List<Item> list(String identity,boolean admin,String kind);
    Result replace(String id,String kind,Batch batch);
    Result save(String id,String kind,Long itemId,Item item);
    Result delete(String id,String kind,long itemId,long version);
    void restore(long id,Snapshot snapshot);
}
