package com.starrainnotes.english.knowledge.service.impl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.english.knowledge.dto.DocumentMetadata;
import com.starrainnotes.english.knowledge.dto.RevisionDto.*;
import com.starrainnotes.english.knowledge.mapper.DocumentMetadataMapper;
import com.starrainnotes.english.knowledge.service.DocumentMetadataService;
import com.starrainnotes.english.taxonomy.service.TaxonomyService;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
@Service @RequiredArgsConstructor
public class DocumentMetadataServiceImpl implements DocumentMetadataService {
    private final DocumentMetadataMapper mapper;
    private final TaxonomyService taxonomy;
    private final ObjectMapper json;
    @Override public void validate(DocumentMetadata value) { validate(value,false); }
    @Override public void validateSnapshot(DocumentMetadata value) { validate(value,true); }
    private List<Long> ids(List<Long> ids,String dimension,boolean historical) { return historical?taxonomy.validateSnapshotIds(ids,dimension):taxonomy.validateIds(ids,dimension); }
    private void validate(DocumentMetadata value,boolean historical) {
        if(value.getPrimaryTopicId()!=null) ids(List.of(value.getPrimaryTopicId()),"TOPIC",historical);
        value.setOtherTopicIds(ids(value.getOtherTopicIds(),"TOPIC",historical));
        value.setGenreIds(ids(value.getGenreIds(),"GENRE",historical)); value.setPurposeIds(ids(value.getPurposeIds(),"PURPOSE",historical));
        if(value.getKeywords()==null) value.setKeywords(List.of());
        if(value.getKeywords().size()>30 || value.getKeywords().stream().anyMatch(k->k==null || k.length()>100)) throw new ApiException("ENGLISH_DOCUMENT_INVALID","关键词数量或长度超出限制",400);
        value.setKeywords(value.getKeywords().stream().map(String::trim).filter(k->!k.isEmpty()).distinct().toList());
    }
    @Override public void load(String kind,long id,DocumentMetadata value) {
        var tags=mapper.tags(kind,id); value.setOtherTopicIds(tags.stream().filter(n->"TOPIC".equals(n.getDimension())).map(n->n.getId()).toList());
        value.setGenreIds(tags.stream().filter(n->"GENRE".equals(n.getDimension())).map(n->n.getId()).toList()); value.setPurposeIds(tags.stream().filter(n->"PURPOSE".equals(n.getDimension())).map(n->n.getId()).toList());
    }
    @Override public void replace(String kind,long id,DocumentMetadata value) { replace(kind,id,value,false); }
    @Override public void replaceSnapshot(String kind,long id,DocumentMetadata value) { replace(kind,id,value,true); }
    private void replace(String kind,long id,DocumentMetadata value,boolean historical) {
        validate(value,historical); var ids=new LinkedHashSet<Long>(); ids.addAll(value.getOtherTopicIds()); ids.addAll(value.getGenreIds()); ids.addAll(value.getPurposeIds());
        mapper.clearTags(kind,id); if(!ids.isEmpty()) mapper.insertTags(kind,id,new ArrayList<>(ids));
    }
    @Override public Page revisions(String kind,long id,int page,int size) {
        int p=Math.max(1,page),s=Math.max(1,Math.min(50,size)); return new Page(mapper.revisions(kind,id,(long)(p-1)*s,s),mapper.revisionCount(kind,id),p,s);
    }
    @Override public Revision revision(String kind,long id,long no) {
        Revision revision=mapper.revision(kind,id,no); if(revision==null) throw new ApiException("ENGLISH_REVISION_NOT_FOUND","历史版本不存在",404);
        try { revision.setSnapshot(json.readTree(revision.getSnapshotJson())); return revision; }
        catch(JsonProcessingException e) { throw new IllegalStateException("Invalid persisted revision",e); }
    }
    @Override public Revision snapshot(String kind,long id,long actor,Object snapshot,String note) {
        if(note!=null && note.length()>500) throw new ApiException("ENGLISH_REVISION_INVALID","版本说明过长",400);
        long no=mapper.nextRevision(kind,id);
        try { mapper.insertRevision(kind,id,no,actor,json.writeValueAsString(snapshot),note); }
        catch(JsonProcessingException e) { throw new IllegalStateException("Unable to serialize revision",e); }
        return revision(kind,id,no);
    }
}
