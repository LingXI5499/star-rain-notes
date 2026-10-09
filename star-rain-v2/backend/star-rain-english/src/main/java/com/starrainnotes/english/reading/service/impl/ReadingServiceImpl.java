package com.starrainnotes.english.reading.service.impl;
import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.english.reading.dto.ReadingDto.*;
import com.starrainnotes.english.reading.mapper.ReadingMapper;
import com.starrainnotes.english.reading.mapper.ReadingEnhancementMapper;
import com.starrainnotes.english.reading.service.ReadingService;
import com.starrainnotes.english.knowledge.service.DocumentMetadataService;
import com.starrainnotes.english.knowledge.utils.EnglishBodyValidator;
import com.starrainnotes.english.taxonomy.service.TaxonomyService;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor
public class ReadingServiceImpl implements ReadingService {
    private final ReadingMapper mapper;
    private final DocumentMetadataService metadata;
    private final TaxonomyService taxonomy;
    private final ReadingEnhancementMapper enhancements;
    @Override @Transactional(readOnly=true) public Page list(boolean admin,String search,int page,int size) { return listFiltered(admin,search,page,size,null,null,null); }
    @Override @Transactional(readOnly=true) public Page listFiltered(boolean admin,String search,int page,int size,Long topicId,Long genreId,Long purposeId) {
        int p=Math.max(1,page),s=Math.max(1,Math.min(50,size)); String term=search==null?"":search.trim(); EnglishBodyValidator.length(term,100);
        var topics=taxonomy.descendants(topicId,"TOPIC");var genres=taxonomy.descendants(genreId,"GENRE");var purposes=taxonomy.descendants(purposeId,"PURPOSE");
        long total=mapper.count(!admin,term,topics,genres,purposes);
        var items=total==0?List.<Article>of():mapper.list(!admin,term,topics,genres,purposes,(long)(p-1)*s,s);
        items.forEach(a->{ metadata.load("READING",a.getId(),a); if(!admin) { a.setBodyMarkdown(null); a.setTranslationZhMarkdown(null); } });
        return new Page(items,total,p,s);
    }
    @Override @Transactional(readOnly=true) public Article get(String identity,boolean admin) {
        Article a=admin?mapper.byId(id(identity)):mapper.publicBySlug(identity); if(a==null) notFound();
        metadata.load("READING",a.getId(),a); Rights rights=mapper.rights(a.getId());
        if(!admin && rights!=null) rights.setRightsBasis(null); a.setRights(rights);
        return a;
    }
    @Override @Transactional public Article create(Request request) {
        validate(request); Article a=fromRequest(request,null);a.setSlug("new-"+UUID.randomUUID());a.setRowVersion(0L);
        mapper.insert(a);a.setSlug("reading-"+a.getId());mapper.finishSlug(a);
        metadata.replace("READING",a.getId(),request);mapper.saveRights(a.getId(),a.getRights(),a.getSourceName(),a.getSourceUrl());
        return get(a.getId().toString(),true);
    }
    @Override @Transactional public Article update(String identity,Request request) { return update(identity,request,false); }
    @Override @Transactional public Article restoreSnapshot(String identity,Request request) { return update(identity,request,true); }
    private Article update(String identity,Request request,boolean historical) {
        validate(request,historical);Article old=mapper.lockById(id(identity));if(old==null) notFound();
        if(request.getRowVersion()==null || !request.getRowVersion().equals(old.getRowVersion())) conflict();
        old.setRights(mapper.rights(old.getId())); Article a=fromRequest(request,old);
        a.setId(old.getId());a.setSlug(old.getSlug());a.setRowVersion(old.getRowVersion());
        if("PUBLISHED".equals(old.getPublishStatus())) requirePublic(a);
        if(mapper.update(a)!=1) conflict();
        if(!Objects.equals(old.getBodyMarkdown(),a.getBodyMarkdown())) { enhancements.stale(a.getId(),"EN"); enhancements.staleAnnotations(a.getId()); }
        if(!Objects.equals(old.getTranslationZhMarkdown(),a.getTranslationZhMarkdown())) enhancements.stale(a.getId(),"ZH");
        if(historical) metadata.replaceSnapshot("READING",a.getId(),request);else metadata.replace("READING",a.getId(),request);mapper.saveRights(a.getId(),a.getRights(),a.getSourceName(),a.getSourceUrl());
        return get(identity,true);
    }
    @Override @Transactional public Article setPublished(String identity,boolean publish) { return setPublished(identity,publish,null); }
    @Override @Transactional public Article setPublished(String identity,boolean publish,Long expectedVersion) {
        Article a=mapper.lockById(id(identity));if(a==null) notFound();a.setRights(mapper.rights(a.getId()));
        if(expectedVersion!=null && !expectedVersion.equals(a.getRowVersion())) conflict();
        if(publish) requirePublic(a);
        if(mapper.setStatus(a.getId(),publish?"PUBLISHED":"WITHDRAWN",a.getRowVersion())!=1) conflict();return get(identity,true);
    }
    @Override @Transactional public void delete(String identity) { Article a=mapper.lockById(id(identity));if(a==null) notFound();if(mapper.delete(a.getId(),a.getRowVersion())!=1) conflict(); }
    private Article fromRequest(Request r,Article old) {
        Article a=new Article(); a.setTitle(EnglishBodyValidator.title(r.getTitle()));a.setSummary(EnglishBodyValidator.blank(r.getSummary()));a.setBodyMarkdown(EnglishBodyValidator.blank(r.getBodyMarkdown()));
        a.setTranslationZhMarkdown(r.getTranslationZhMarkdown());a.setPrimaryTopicId(r.getPrimaryTopicId());
        a.setSourceName(r.getSourceName());a.setSourceUrl(r.getSourceUrl());a.setSortOrder(r.getSortOrder()==null?0:r.getSortOrder());a.setContentOrigin(r.getContentOrigin()==null?(old==null?"EXTERNAL":old.getContentOrigin()):r.getContentOrigin());
        Rights rights=r.getRights();if(rights==null) rights=old==null?new Rights():old.getRights();if(rights==null) rights=new Rights();if(rights.getRightsStatus()==null) rights.setRightsStatus("PENDING");
        if(old!=null && r.getRights()==null && (!Objects.equals(old.getBodyMarkdown(),a.getBodyMarkdown()) || !Objects.equals(old.getSourceUrl(),a.getSourceUrl()))) rights.setRightsStatus("PENDING");
        a.setRights(rights);return a;
    }
    private void validate(Request r) { validate(r,false); }
    private void validate(Request r,boolean historical) {
        if(r==null) throw new ApiException("ENGLISH_DOCUMENT_INVALID","内容不能为空",400);
        EnglishBodyValidator.length(r.getTitle(),200);EnglishBodyValidator.length(r.getSummary(),1000);EnglishBodyValidator.length(r.getBodyMarkdown(),200000);EnglishBodyValidator.length(r.getTranslationZhMarkdown(),200000);
        EnglishBodyValidator.length(r.getSourceName(),200);EnglishBodyValidator.length(r.getSourceUrl(),500);
        if(r.getSourceUrl()!=null && !r.getSourceUrl().isBlank() && !r.getSourceUrl().matches("https?://[^\\s]+")) throw new ApiException("ENGLISH_DOCUMENT_INVALID","来源地址仅支持 HTTP 或 HTTPS",400);
        if(r.getContentOrigin()!=null && !List.of("ORIGINAL","EXTERNAL").contains(r.getContentOrigin())) throw new ApiException("ENGLISH_DOCUMENT_INVALID","无效的来源类型",400);
        if(r.getRights()!=null) { Rights rights=r.getRights(); if(rights.getRightsStatus()!=null && !List.of("PENDING","CLEARED","BLOCKED").contains(rights.getRightsStatus())) throw new ApiException("ENGLISH_DOCUMENT_INVALID","无效的版权状态",400);
            EnglishBodyValidator.length(rights.getRightsBasis(),1000);EnglishBodyValidator.length(rights.getOriginalAuthor(),200);EnglishBodyValidator.length(rights.getLicenseNotice(),10000); }
        if(historical) metadata.validateSnapshot(r);else metadata.validate(r);
    }
    public static void requirePublic(Article a) {
        EnglishBodyValidator.requireBody(a.getBodyMarkdown());
        if(a.getRights()!=null && "BLOCKED".equals(a.getRights().getRightsStatus())) uncleared();
        if(!"ORIGINAL".equals(a.getContentOrigin()) && (a.getRights()==null || !"CLEARED".equals(a.getRights().getRightsStatus()) || a.getRights().getRightsBasis()==null || a.getRights().getRightsBasis().isBlank())) uncleared();
    }
    private static void uncleared() { throw new ApiException("READING_RIGHTS_UNCLEARED","公开外部作品前请核查权利依据",409); }
    private static void conflict() { throw new ApiException("READING_VERSION_CONFLICT","内容已变化，请刷新后再编辑",409); }
    private static void notFound() { throw new ApiException("ENGLISH_DOCUMENT_NOT_FOUND","文章不存在或未公开",404); }
    public static long id(String value) { try { long id=Long.parseLong(value);if(id<=0) throw new NumberFormatException();return id; } catch(NumberFormatException e) { throw new ApiException("ENGLISH_ID_INVALID","无效的内容编号",400); } }
}
