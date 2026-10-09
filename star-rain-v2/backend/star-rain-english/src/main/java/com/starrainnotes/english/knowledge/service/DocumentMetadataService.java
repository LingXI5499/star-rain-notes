package com.starrainnotes.english.knowledge.service;
import com.starrainnotes.english.knowledge.dto.DocumentMetadata;
import com.starrainnotes.english.knowledge.dto.RevisionDto.*;
public interface DocumentMetadataService {
    void validate(DocumentMetadata value);
    void validateSnapshot(DocumentMetadata value);
    void replaceSnapshot(String kind,long id,DocumentMetadata value);
    void load(String kind,long id,DocumentMetadata value);
    void replace(String kind,long id,DocumentMetadata value);
    Page revisions(String kind,long id,int page,int size);
    Revision revision(String kind,long id,long no);
    Revision snapshot(String kind,long id,long actor,Object snapshot,String note);
}
