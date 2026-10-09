package com.starrainnotes.english.writing.article.service.impl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.account.api.CurrentActorApi;
import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.english.api.event.EnglishSearchContentChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
import com.starrainnotes.english.knowledge.dto.RevisionDto;
import com.starrainnotes.english.knowledge.service.DocumentMetadataService;
import com.starrainnotes.english.knowledge.utils.EnglishBodyValidator;
import com.starrainnotes.english.reading.service.impl.ReadingServiceImpl;
import com.starrainnotes.english.taxonomy.service.TaxonomyService;
import com.starrainnotes.english.writing.article.dto.WritingArticleDto.*;
import com.starrainnotes.english.writing.article.mapper.WritingArticleMapper;
import com.starrainnotes.english.writing.article.service.WritingArticleService;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor
public class WritingArticleServiceImpl implements WritingArticleService {
    private final WritingArticleMapper mapper; private final DocumentMetadataService metadata;
    private final TaxonomyService taxonomy; private final CurrentActorApi actor; private final ObjectMapper json;
    private final ApplicationEventPublisher events;
    @Override @Transactional(readOnly=true) public Page list(boolean mine,String search,String state,Long topic,Long genre,Long purpose,int page,int size) {
        Long owner=mine?owner():null;String term=search==null?"":search.trim();EnglishBodyValidator.length(term,100);
        if(state!=null && !state.isBlank() && !List.of("DRAFT","COMPLETED").contains(state)) invalid("状态无效");
        var topics=taxonomy.descendants(topic,"TOPIC");var genres=taxonomy.descendants(genre,"GENRE");var purposes=taxonomy.descendants(purpose,"PURPOSE");
        int p=Math.max(1,page),s=Math.max(1,Math.min(50,size));long total=mapper.count(owner,term,state,topics,genres,purposes);
        List<Article> rows=total==0?List.of():mapper.list(owner,term,state,topics,genres,purposes,(long)(p-1)*s,s);
        rows.forEach(a->{ enrich(a);a.setBodyMarkdown(null);a.setTranslationZhMarkdown(null); });return new Page(rows,total,p,s);
    }
    @Override @Transactional(readOnly=true) public Article get(String id) { Article a=mapper.owned(ReadingServiceImpl.id(id),owner());if(a==null) missing();return enrich(a); }
    @Override @Transactional(readOnly=true) public Article getPublic(String slug) { Article a=mapper.publicBySlug(slug);if(a==null) missing();return enrich(a); }
    @Override @Transactional public Article create(Request r) {
        validate(r);Article a=apply(r,new Article());a.setOwnerAccountId(owner());a.setPublicEligible(canPublish());a.setSlug("new-"+UUID.randomUUID());a.setRowVersion(0L);
        mapper.insert(a);a.setSlug("writing-"+a.getId());mapper.finishSlug(a);metadata.replace("WRITING",a.getId(),r);return get(a.getId().toString());
    }
    @Override @Transactional public Article save(String id,Request r) { return save(id,r,false); }
    private Article save(String id,Request r,boolean historical) {
        validate(r,historical);Article old=lock(id,r.getRowVersion());Article a=apply(r,old);
        if("PUBLIC".equals(old.getVisibility())) { EnglishBodyValidator.requireBody(a.getBodyMarkdown()); if(!canPublish()) a.setVisibility("PRIVATE"); }
        if(!EnglishBodyValidator.hasMeaningfulText(a.getBodyMarkdown())) { a.setState("DRAFT");a.setVisibility("PRIVATE"); }
        if(mapper.update(a)!=1) conflict();if(historical)metadata.replaceSnapshot("WRITING",a.getId(),r);else metadata.replace("WRITING",a.getId(),r);changed(a.getId());return get(id);
    }
    @Override @Transactional public Article complete(String id,Long version) {
        Article a=lock(id,version);EnglishBodyValidator.requireBody(a.getBodyMarkdown());change(a,"COMPLETED","PRIVATE");
        Article completed=get(id);metadata.snapshot("WRITING",a.getId(),owner(),completed,"完成写作");return completed;
    }
    @Override @Transactional public Article publish(String id,Long version,boolean publish) {
        Article a=lock(id,version);
        if(publish) { if(!a.isPublicEligible() || !canPublish()) throw new ApiException("WRITING_PUBLISH_FORBIDDEN","只有具备发布权限的管理员原创可以公开",403);
            EnglishBodyValidator.requireBody(a.getBodyMarkdown());if(!"COMPLETED".equals(a.getState())) invalid("请先完成文章"); }
        change(a,a.getState(),publish?"PUBLIC":"PRIVATE");Article saved=get(id);
        if(publish) metadata.snapshot("WRITING",a.getId(),owner(),saved,"公开原创");return saved;
    }
    @Override @Transactional public void delete(String id) { Article a=lockOwned(id);if(mapper.delete(a.getId(),owner(),a.getRowVersion())!=1) conflict();changed(a.getId()); }
    @Override @Transactional(readOnly=true) public RevisionDto.Page revisions(String id,int page,int size) { Article a=get(id);return metadata.revisions("WRITING",a.getId(),page,size); }
    @Override @Transactional(readOnly=true) public RevisionDto.Revision revision(String id,long no) { Article a=get(id);return metadata.revision("WRITING",a.getId(),no); }
    @Override @Transactional public RevisionDto.Revision snapshot(String id,RevisionDto.Request r) { lock(id,r.getRowVersion());return metadata.snapshot("WRITING",ReadingServiceImpl.id(id),owner(),get(id),r.getChangeNote()); }
    @Override @Transactional public Article restore(String id,long no,RevisionDto.Request r) {
        Article old=lock(id,r.getRowVersion());Request restore=json.convertValue(revision(id,no).getSnapshot(),Request.class);restore.setRowVersion(old.getRowVersion());
        Article saved=save(id,restore,true);metadata.snapshot("WRITING",saved.getId(),owner(),saved,"恢复版本 "+no);return saved;
    }
    private Article enrich(Article a) { metadata.load("WRITING",a.getId(),a);
        try { a.setKeywords(a.getKeywordsJson()==null?List.of():json.readValue(a.getKeywordsJson(),new TypeReference<List<String>>(){})); }
        catch(JsonProcessingException e) { throw new IllegalStateException("Invalid persisted writing keywords",e); }return a;
    }
    private Article apply(Request r,Article a) {
        a.setTitle(EnglishBodyValidator.title(r.getTitle()));a.setSummary(EnglishBodyValidator.blank(r.getSummary()));a.setBodyMarkdown(EnglishBodyValidator.blank(r.getBodyMarkdown()));a.setTranslationZhMarkdown(r.getTranslationZhMarkdown());a.setPrimaryTopicId(r.getPrimaryTopicId());
        if(a.getState()==null) a.setState("DRAFT");if(a.getVisibility()==null) a.setVisibility("PRIVATE");
        try { a.setKeywordsJson(json.writeValueAsString(r.getKeywords())); } catch(JsonProcessingException e) { throw new IllegalStateException("Unable to serialize writing keywords",e); }return a;
    }
    private void validate(Request r) { validate(r,false); }
    private void validate(Request r,boolean historical) { if(r==null) invalid("内容不能为空");EnglishBodyValidator.length(r.getTitle(),200);EnglishBodyValidator.length(r.getSummary(),1000);EnglishBodyValidator.length(r.getBodyMarkdown(),200000);EnglishBodyValidator.length(r.getTranslationZhMarkdown(),200000);if(historical)metadata.validateSnapshot(r);else metadata.validate(r); }
    private Article lockOwned(String id) { Article a=mapper.locked(ReadingServiceImpl.id(id),owner());if(a==null) missing();return a; }
    private Article lock(String id,Long version) { Article a=lockOwned(id);if(version==null || !version.equals(a.getRowVersion())) conflict();return a; }
    private void change(Article a,String state,String visibility) { if(mapper.status(a.getId(),owner(),a.getRowVersion(),state,visibility)!=1) conflict();changed(a.getId()); }
    private void changed(Long id) { events.publishEvent(new EnglishSearchContentChangedEvent("ENGLISH_WRITING",id)); }
    private long owner() { return actor.current().getAccountId(); }
    private boolean canPublish() { var current=actor.current();return current.getRoles()!=null && (current.getRoles().contains("ADMIN") || current.getRoles().contains("SUPER_ADMIN")) && current.getPermissions()!=null && current.getPermissions().contains("english:content-publish"); }
    private static void invalid(String message) { throw new ApiException("WRITING_ARTICLE_INVALID",message,400); }
    private static void conflict() { throw new ApiException("WRITING_VERSION_CONFLICT","其他页面已修改文章；当前草稿已保留，请重新读取后合并",409); }
    private static void missing() { throw new ApiException("WRITING_ARTICLE_NOT_FOUND","文章不存在",404); }
}
