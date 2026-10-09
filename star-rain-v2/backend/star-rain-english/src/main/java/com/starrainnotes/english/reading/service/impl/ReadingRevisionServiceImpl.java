package com.starrainnotes.english.reading.service.impl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.english.knowledge.dto.RevisionDto.*;
import com.starrainnotes.english.knowledge.service.DocumentMetadataService;
import com.starrainnotes.english.reading.dto.ReadingDto.Article;
import com.starrainnotes.english.reading.dto.ReadingEnhancementDto.Snapshot;
import com.starrainnotes.english.reading.mapper.ReadingMapper;
import com.starrainnotes.english.reading.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor
public class ReadingRevisionServiceImpl implements ReadingRevisionService {
    private final ReadingService reading; private final ReadingMapper mapper;
    private final ReadingEnhancementService enhancements; private final DocumentMetadataService metadata;
    private final CurrentActorApi actor; private final ObjectMapper json;
    @Override @Transactional(readOnly=true) public Page list(String id,int page,int size) { Article a=reading.get(id,true);return metadata.revisions("READING",a.getId(),page,size); }
    @Override @Transactional(readOnly=true) public Revision get(String id,long no) { Article a=reading.get(id,true);return metadata.revision("READING",a.getId(),no); }
    @Override @Transactional public Revision snapshot(String id,Request request) { lock(id,request);return capture(id,request.getChangeNote()); }
    @Override @Transactional public Article restore(String id,long no,Request request) {
        lock(id,request);Snapshot saved=json.convertValue(metadata.revision("READING",ReadingServiceImpl.id(id),no).getSnapshot(),Snapshot.class);
        var update=json.convertValue(saved.article(),com.starrainnotes.english.reading.dto.ReadingDto.Request.class);
        update.setRowVersion(request.getRowVersion());Article article=reading.restoreSnapshot(id,update);
        enhancements.restore(article.getId(),saved);capture(id,"恢复版本 "+no);return article;
    }
    private void lock(String id,Request request) { Article a=mapper.lockById(ReadingServiceImpl.id(id));
        if(a==null) throw new ApiException("ENGLISH_DOCUMENT_NOT_FOUND","文章不存在",404);
        if(request==null || request.getRowVersion()==null || !request.getRowVersion().equals(a.getRowVersion())) throw new ApiException("READING_VERSION_CONFLICT","内容已变化，请刷新后再编辑",409);
    }
    private Revision capture(String id,String note) {
        Article a=reading.get(id,true);Snapshot snapshot=new Snapshot(a,enhancements.list(id,true,"alignments"),enhancements.list(id,true,"annotations"),enhancements.list(id,true,"vocabulary"));
        return metadata.snapshot("READING",a.getId(),actor.current().getAccountId(),snapshot,note);
    }
}
