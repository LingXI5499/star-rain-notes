/*
 * WritingArticleServiceImpl：英语学习模块的业务实现文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：CurrentActorApi、ApiException、EnglishSearchContentChangedEvent、RevisionDto、DocumentMetadataService、EnglishBodyValidator、ReadingServiceImpl。
 * 阅读入口：list（列出写作Article）；get（读取写作Article）；getPublic（读取公开）；create（创建写作Article）。
 */

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
import com.starrainnotes.review.api.*;
import com.starrainnotes.review.api.dto.ReviewSubmissionCommand;
import org.springframework.beans.factory.ObjectProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * 写作Article业务服务实现，属于英语学习模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service @RequiredArgsConstructor
public class WritingArticleServiceImpl implements WritingArticleService {
    /**
     * 数据映射。
     * 协作类型：<code>WritingArticleMapper</code>。
     */
    private final WritingArticleMapper mapper;
    private final ObjectProvider<ReviewSubmissionApi> reviewProvider;
    /**
     * 已通过元数据校验的上传信息。
     * 协作类型：<code>DocumentMetadataService</code>。
     */
    private final DocumentMetadataService metadata;
    /**
     * taxonomy。
     * 协作类型：<code>TaxonomyService</code>。
     */
    private final TaxonomyService taxonomy;
    /**
     * 当前操作对应的身份对象。
     * 协作类型：<code>CurrentActorApi</code>。
     */
    private final CurrentActorApi actor;
    /**
     * JSON 读写或映射协作对象。
     * 协作类型：<code>ObjectMapper</code>。
     */
    private final ObjectMapper json;
    /**
     * 事件集合。
     */
    private final ApplicationEventPublisher events;
    /**
     * 列出写作Article。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>taxonomy.descendants</code>、<code>mapper.count</code>、<code>mapper.list</code>。
     *
     * @param mine mine；类型为 <code>boolean</code>
     * @param search 搜索；类型为 <code>String</code>
     * @param state 状态；类型为 <code>String</code>
     * @param topic 专题；类型为 <code>Long</code>
     * @param genre genre；类型为 <code>Long</code>
     * @param purpose purpose；类型为 <code>Long</code>
     * @param page 分页页码；类型为 <code>int</code>
     * @param size 大小；类型为 <code>int</code>
     * @return 类型为 <code>Page</code> 的处理结果
     */
    @Override @Transactional(readOnly=true) public Page list(boolean mine,String search,String state,Long topic,Long genre,Long purpose,int page,int size) {
        Long owner=mine?owner():null;String term=search==null?"":search.trim();EnglishBodyValidator.length(term,100);
        if(state!=null && !state.isBlank() && !List.of("DRAFT","COMPLETED","PENDING","REJECTED","PUBLISHED").contains(state)) invalid("状态无效");
        var topics=taxonomy.descendants(topic,"TOPIC");var genres=taxonomy.descendants(genre,"GENRE");var purposes=taxonomy.descendants(purpose,"PURPOSE");
        int p=Math.max(1,page),s=Math.max(1,Math.min(50,size));long total=mapper.count(owner,term,state,topics,genres,purposes);
        List<Article> rows=total==0?List.of():mapper.list(owner,term,state,topics,genres,purposes,(long)(p-1)*s,s);
        rows.forEach(a->{ enrich(a);a.setBodyMarkdown(null);a.setTranslationZhMarkdown(null); });return new Page(rows,total,p,s);
    }
    /**
     * 读取写作Article。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.owned</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @return 类型为 <code>Article</code> 的处理结果
     */
    @Override @Transactional(readOnly=true) public Article get(String id) { Article a=mapper.owned(ReadingServiceImpl.id(id),owner());if(a==null) missing();return enrich(a); }
    /**
     * 读取公开。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.publicBySlug</code>。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @return 类型为 <code>Article</code> 的处理结果
     */
    @Override @Transactional(readOnly=true) public Article getPublic(String slug) { Article a=mapper.publicBySlug(slug);if(a==null) missing();return enrich(a); }
    /**
     * 创建写作Article。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.insert</code>、<code>mapper.finishSlug</code>、<code>metadata.replace</code>。
     *
     * @param r r；类型为 <code>Request</code>
     * @return 类型为 <code>Article</code> 的处理结果
     */
    @Override @Transactional public Article create(Request r) {
        validate(r);Article a=apply(r,new Article());a.setOwnerAccountId(owner());a.setPublicEligible(canPublish());a.setSlug("new-"+UUID.randomUUID());a.setRowVersion(0L);
        mapper.insert(a);a.setSlug("writing-"+a.getId());mapper.finishSlug(a);metadata.replace("WRITING",a.getId(),r);return get(a.getId().toString());
    }
    /**
     * 保存写作Article。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 关键处理步骤调用 <code>save</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param r r；类型为 <code>Request</code>
     * @return 类型为 <code>Article</code> 的处理结果
     */
    @Override @Transactional public Article save(String id,Request r) { return save(id,r,false); }
    /**
     * 保存写作Article。
     * 主要协作调用：<code>mapper.update</code>、<code>metadata.replaceSnapshot</code>、<code>metadata.replace</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param r r；类型为 <code>Request</code>
     * @param historical historical；类型为 <code>boolean</code>
     * @return 类型为 <code>Article</code> 的处理结果
     */
    private Article save(String id,Request r,boolean historical) {
        validate(r,historical);Article old=lock(id,r.getRowVersion());requireEditable(old);Article a=apply(r,old);
        a.setState("DRAFT");a.setVisibility("PRIVATE");
        if("PUBLIC".equals(old.getVisibility())) { EnglishBodyValidator.requireBody(a.getBodyMarkdown()); if(!canPublish()) a.setVisibility("PRIVATE"); }
        if(!EnglishBodyValidator.hasMeaningfulText(a.getBodyMarkdown())) { a.setState("DRAFT");a.setVisibility("PRIVATE"); }
        if(mapper.update(a)!=1) conflict();if(historical)metadata.replaceSnapshot("WRITING",a.getId(),r);else metadata.replace("WRITING",a.getId(),r);changed(a.getId());return get(id);
    }
    /**
     * 完成写作Article。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>metadata.snapshot</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param version 版本；类型为 <code>Long</code>
     * @return 类型为 <code>Article</code> 的处理结果
     */
    @Override @Transactional public Article complete(String id,Long version) {
        Article a=lock(id,version);requireEditable(a);EnglishBodyValidator.requireBody(a.getBodyMarkdown());change(a,"COMPLETED","PRIVATE");
        Article completed=get(id);metadata.snapshot("WRITING",a.getId(),owner(),completed,"完成写作");return completed;
    }
    /**
     * 发布写作Article。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>metadata.snapshot</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param version 版本；类型为 <code>Long</code>
     * @param publish 发布；类型为 <code>boolean</code>
     * @return 类型为 <code>Article</code> 的处理结果
     */
    @Override @Transactional public Article publish(String id,Long version,boolean publish) {
        Article a=lock(id,version);
        if("PENDING".equals(a.getState())) invalid("请在审核中心处理待审核文章");
        if(publish) { if(!a.isPublicEligible() || !canPublish()) throw new ApiException("WRITING_PUBLISH_FORBIDDEN","只有具备发布权限的管理员原创可以公开",403);
            EnglishBodyValidator.requireBody(a.getBodyMarkdown());if(!"COMPLETED".equals(a.getState())) invalid("请先完成文章"); }
        change(a,publish?a.getState():"DRAFT",publish?"PUBLIC":"PRIVATE");Article saved=get(id);
        if(publish) metadata.snapshot("WRITING",a.getId(),owner(),saved,"公开原创");return saved;
    }
    /**
     * 删除写作Article。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.delete</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     */
    @Override @Transactional public void delete(String id) { Article a=lockOwned(id);requireEditable(a);if(mapper.delete(a.getId(),owner(),a.getRowVersion())!=1) conflict();changed(a.getId()); }
    /**
     * 修订集合处理入口。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>metadata.revisions</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param page 分页页码；类型为 <code>int</code>
     * @param size 大小；类型为 <code>int</code>
     * @return 类型为 <code>RevisionDto.Page</code> 的处理结果
     */
    @Override @Transactional(readOnly=true) public RevisionDto.Page revisions(String id,int page,int size) { Article a=get(id);return metadata.revisions("WRITING",a.getId(),page,size); }
    /**
     * 修订处理入口。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>metadata.revision</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param no 不含；类型为 <code>long</code>
     * @return 类型为 <code>RevisionDto.Revision</code> 的处理结果
     */
    @Override @Transactional(readOnly=true) public RevisionDto.Revision revision(String id,long no) { Article a=get(id);return metadata.revision("WRITING",a.getId(),no); }
    /**
     * 快照处理入口。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>metadata.snapshot</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param r r；类型为 <code>RevisionDto.Request</code>
     * @return 类型为 <code>RevisionDto.Revision</code> 的处理结果
     */
    @Override @Transactional public RevisionDto.Revision snapshot(String id,RevisionDto.Request r) { lock(id,r.getRowVersion());return metadata.snapshot("WRITING",ReadingServiceImpl.id(id),owner(),get(id),r.getChangeNote()); }
    /**
     * 恢复写作Article。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>json.convertValue</code>、<code>metadata.snapshot</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param no 不含；类型为 <code>long</code>
     * @param r r；类型为 <code>RevisionDto.Request</code>
     * @return 类型为 <code>Article</code> 的处理结果
     */
    @Override @Transactional public Article restore(String id,long no,RevisionDto.Request r) {
        Article old=lock(id,r.getRowVersion());Request restore=json.convertValue(revision(id,no).getSnapshot(),Request.class);restore.setRowVersion(old.getRowVersion());
        Article saved=save(id,restore,true);metadata.snapshot("WRITING",saved.getId(),owner(),saved,"恢复版本 "+no);return saved;
    }
    /**
     * enrich处理入口。
     * 主要协作调用：<code>metadata.load</code>、<code>json.readValue</code>。
     * 实现中的显式失败类型包括 <code>IllegalStateException</code>；具体触发条件见对应分支。
     *
     * @param a a；类型为 <code>Article</code>
     * @return 类型为 <code>Article</code> 的处理结果
     */
    private Article enrich(Article a) { metadata.load("WRITING",a.getId(),a);
        try { a.setKeywords(a.getKeywordsJson()==null?List.of():json.readValue(a.getKeywordsJson(),new TypeReference<List<String>>()
    /**
     * 当前调用使用的匿名协作实现，属于英语学习模块。
     * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
     */
    {})); }
        catch(JsonProcessingException e) { throw new IllegalStateException("Invalid persisted writing keywords",e); }return a;
    }
    /**
     * 应用写作Article。
     * 主要协作调用：<code>json.writeValueAsString</code>。
     * 实现中的显式失败类型包括 <code>IllegalStateException</code>；具体触发条件见对应分支。
     *
     * @param r r；类型为 <code>Request</code>
     * @param a a；类型为 <code>Article</code>
     * @return 类型为 <code>Article</code> 的处理结果
     */
    private Article apply(Request r,Article a) {
        a.setTitle(EnglishBodyValidator.title(r.getTitle()));a.setSummary(EnglishBodyValidator.blank(r.getSummary()));a.setBodyMarkdown(EnglishBodyValidator.blank(r.getBodyMarkdown()));a.setTranslationZhMarkdown(r.getTranslationZhMarkdown());a.setPrimaryTopicId(r.getPrimaryTopicId());
        if(a.getState()==null) a.setState("DRAFT");if(a.getVisibility()==null) a.setVisibility("PRIVATE");
        try { a.setKeywordsJson(json.writeValueAsString(r.getKeywords())); } catch(JsonProcessingException e) { throw new IllegalStateException("Unable to serialize writing keywords",e); }return a;
    }
    /**
     * 校验写作Article。
     * 关键处理步骤调用 <code>validate</code>。
     *
     * @param r r；类型为 <code>Request</code>
     */
    private void validate(Request r) { validate(r,false); }
    /**
     * 校验写作Article。
     * 主要协作调用：<code>metadata.validateSnapshot</code>、<code>metadata.validate</code>。
     *
     * @param r r；类型为 <code>Request</code>
     * @param historical historical；类型为 <code>boolean</code>
     */
    private void validate(Request r,boolean historical) { if(r==null) invalid("内容不能为空");EnglishBodyValidator.length(r.getTitle(),200);EnglishBodyValidator.length(r.getSummary(),1000);EnglishBodyValidator.length(r.getBodyMarkdown(),200000);EnglishBodyValidator.length(r.getTranslationZhMarkdown(),200000);if(historical)metadata.validateSnapshot(r);else metadata.validate(r); }
    /**
     * 锁Owned处理入口。
     * 主要协作调用：<code>mapper.locked</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @return 类型为 <code>Article</code> 的处理结果
     */
    private Article lockOwned(String id) { Article a=mapper.locked(ReadingServiceImpl.id(id),owner());if(a==null) missing();return a; }
    /**
     * 锁处理入口。
     * 关键处理步骤调用 <code>lockOwned</code>、<code>version.equals</code>、<code>a.getRowVersion</code>、<code>conflict</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param version 版本；类型为 <code>Long</code>
     * @return 类型为 <code>Article</code> 的处理结果
     */
    private Article lock(String id,Long version) { Article a=lockOwned(id);if(version==null || !version.equals(a.getRowVersion())) conflict();return a; }
    /**
     * change处理入口。
     * 主要协作调用：<code>mapper.status</code>。
     *
     * @param a a；类型为 <code>Article</code>
     * @param state 状态；类型为 <code>String</code>
     * @param visibility visibility；类型为 <code>String</code>
     */
    private void change(Article a,String state,String visibility) { if(mapper.status(a.getId(),owner(),a.getRowVersion(),state,visibility)!=1) conflict();changed(a.getId()); }
    /**
     * changed处理入口。
     * 主要协作调用：<code>events.publishEvent</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     */
    private void changed(Long id) { events.publishEvent(new EnglishSearchContentChangedEvent("ENGLISH_WRITING",id)); }
    /**
     * owner处理入口。
     * 主要协作调用：<code>actor.current</code>。
     * @return 类型为 <code>long</code> 的处理结果
     */

    @Override @Transactional
    public Article submit(String id,Long version) {
        Article a=lock(id,version);requireEditable(a);
        if(a.getTitle()==null || a.getTitle().isBlank() || "Untitled Article".equals(a.getTitle())) invalid("请填写文章标题");
        EnglishBodyValidator.requireBody(a.getBodyMarkdown());
        RevisionDto.Revision revision=metadata.snapshot("WRITING",a.getId(),owner(),get(id),"提交随笔审核");
        reviewProvider.getObject().submit(ReviewSubmissionCommand.builder()
            .reviewType("english.essay.publish").targetModule("ENGLISH").targetType("ESSAY")
            .targetId(a.getId()).targetRevisionRef(Long.toString(revision.getRevisionNo()))
            .targetDisplayName(a.getTitle()).applicantAccountId(owner()).build());
        change(a,"PENDING","PRIVATE");return get(id);
    }

    @Override @Transactional
    public Article cancelReview(String id,Long version) {
        Article a=lock(id,version);
        if(!"PENDING".equals(a.getState())) invalid("文章当前不在审核中");
        ReviewTargetKey key=ReviewTargetKey.builder().targetModule("ENGLISH").targetType("ESSAY").targetId(a.getId()).build();
        var pending=reviewProvider.getObject().findActivePending(key,"english.essay.publish")
            .orElseThrow(()->new ApiException("WRITING_REVIEW_NOT_PENDING","审核已处理，请刷新文章",409));
        reviewProvider.getObject().cancelByApplicant(pending.getReviewRequestId(),owner());
        change(a,"DRAFT","PRIVATE");return get(id);
    }

    private static void requireEditable(Article article) {
        if("PENDING".equals(article.getState())) invalid("审核期间不能修改或删除，请先取消送审");
        if("PUBLIC".equals(article.getVisibility())) invalid("请先撤回已发布文章，再修改或删除");
    }

    private long owner() { return actor.current().getAccountId(); }
    /**
     * 判断能否发布。
     * 主要协作调用：<code>actor.current</code>。
     * @return 本方法条件是否成立的判断结果
     */
    private boolean canPublish() { var current=actor.current();return current.getRoles()!=null && (current.getRoles().contains("ADMIN") || current.getRoles().contains("SUPER_ADMIN")) && current.getPermissions()!=null && current.getPermissions().contains("english:content-publish"); }
    /**
     * 无效处理入口。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     *
     * @param message 留言；类型为 <code>String</code>
     */
    private static void invalid(String message) { throw new ApiException("WRITING_ARTICLE_INVALID",message,400); }
    /**
     * 冲突处理入口。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     */
    private static void conflict() { throw new ApiException("WRITING_VERSION_CONFLICT","其他页面已修改文章；当前草稿已保留，请重新读取后合并",409); }
    /**
     * 缺失处理入口。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     */
    private static void missing() { throw new ApiException("WRITING_ARTICLE_NOT_FOUND","文章不存在",404); }
}
