/*
 * ReadingServiceImpl：英语学习模块的业务实现文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：ApiException、EnglishSearchContentChangedEvent、ReadingMapper、ReadingEnhancementMapper、ReadingService、DocumentMetadataService、EnglishBodyValidator。
 * 阅读入口：list（列出阅读）；listFiltered（列出Filtered）；get（读取阅读）；create（创建阅读）。
 */

package com.starrainnotes.english.reading.service.impl;
import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.english.api.event.EnglishSearchContentChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
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
/**
 * 阅读业务服务实现，属于英语学习模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service @RequiredArgsConstructor
public class ReadingServiceImpl implements ReadingService {
    /**
     * 数据映射。
     * 协作类型：<code>ReadingMapper</code>。
     */
    private final ReadingMapper mapper;
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
     * enhancements。
     * 协作类型：<code>ReadingEnhancementMapper</code>。
     */
    private final ReadingEnhancementMapper enhancements;
    /**
     * 事件集合。
     */
    private final ApplicationEventPublisher events;
    /**
     * 列出阅读。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 关键处理步骤调用 <code>listFiltered</code>。
     *
     * @param admin 后台；类型为 <code>boolean</code>
     * @param search 搜索；类型为 <code>String</code>
     * @param page 分页页码；类型为 <code>int</code>
     * @param size 大小；类型为 <code>int</code>
     * @return 类型为 <code>Page</code> 的处理结果
     */
    @Override @Transactional(readOnly=true) public Page list(boolean admin,String search,int page,int size) { return listFiltered(admin,search,page,size,null,null,null); }
    /**
     * 列出Filtered。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>taxonomy.descendants</code>、<code>mapper.count</code>、<code>mapper.list</code>、<code>metadata.load</code>。
     *
     * @param admin 后台；类型为 <code>boolean</code>
     * @param search 搜索；类型为 <code>String</code>
     * @param page 分页页码；类型为 <code>int</code>
     * @param size 大小；类型为 <code>int</code>
     * @param topicId 专题标识；类型为 <code>Long</code>
     * @param genreId genre标识；类型为 <code>Long</code>
     * @param purposeId purpose标识；类型为 <code>Long</code>
     * @return 类型为 <code>Page</code> 的处理结果
     */
    @Override @Transactional(readOnly=true) public Page listFiltered(boolean admin,String search,int page,int size,Long topicId,Long genreId,Long purposeId) {
        int p=Math.max(1,page),s=Math.max(1,Math.min(50,size)); String term=search==null?"":search.trim(); EnglishBodyValidator.length(term,100);
        var topics=taxonomy.descendants(topicId,"TOPIC");var genres=taxonomy.descendants(genreId,"GENRE");var purposes=taxonomy.descendants(purposeId,"PURPOSE");
        long total=mapper.count(!admin,term,topics,genres,purposes);
        var items=total==0?List.<Article>of():mapper.list(!admin,term,topics,genres,purposes,(long)(p-1)*s,s);
        items.forEach(a->{ metadata.load("READING",a.getId(),a); if(!admin) { a.setBodyMarkdown(null); a.setTranslationZhMarkdown(null); } });
        return new Page(items,total,p,s);
    }
    /**
     * 读取阅读。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.byId</code>、<code>mapper.publicBySlug</code>、<code>metadata.load</code>、<code>mapper.rights</code>。
     *
     * @param identity 身份；类型为 <code>String</code>
     * @param admin 后台；类型为 <code>boolean</code>
     * @return 类型为 <code>Article</code> 的处理结果
     */
    @Override @Transactional(readOnly=true) public Article get(String identity,boolean admin) {
        Article a=admin?mapper.byId(id(identity)):mapper.publicBySlug(identity); if(a==null) notFound();
        metadata.load("READING",a.getId(),a); Rights rights=mapper.rights(a.getId());
        if(!admin && rights!=null) rights.setRightsBasis(null); a.setRights(rights);
        return a;
    }
    /**
     * 创建阅读。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.insert</code>、<code>mapper.finishSlug</code>、<code>metadata.replace</code>、<code>mapper.saveRights</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>Request</code>
     * @return 类型为 <code>Article</code> 的处理结果
     */
    @Override @Transactional public Article create(Request request) {
        validate(request); Article a=fromRequest(request,null);a.setSlug("new-"+UUID.randomUUID());a.setRowVersion(0L);
        mapper.insert(a);a.setSlug("reading-"+a.getId());mapper.finishSlug(a);
        metadata.replace("READING",a.getId(),request);mapper.saveRights(a.getId(),a.getRights(),a.getSourceName(),a.getSourceUrl());
        return get(a.getId().toString(),true);
    }
    /**
     * 更新阅读。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 关键处理步骤调用 <code>update</code>。
     *
     * @param identity 身份；类型为 <code>String</code>
     * @param request 本次操作的请求参数对象；类型为 <code>Request</code>
     * @return 类型为 <code>Article</code> 的处理结果
     */
    @Override @Transactional public Article update(String identity,Request request) { return update(identity,request,false); }
    /**
     * 恢复快照。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 关键处理步骤调用 <code>update</code>。
     *
     * @param identity 身份；类型为 <code>String</code>
     * @param request 本次操作的请求参数对象；类型为 <code>Request</code>
     * @return 类型为 <code>Article</code> 的处理结果
     */
    @Override @Transactional public Article restoreSnapshot(String identity,Request request) { return update(identity,request,true); }
    /**
     * 更新阅读。
     * 主要协作调用：<code>mapper.lockById</code>、<code>mapper.rights</code>、<code>mapper.update</code>、<code>enhancements.stale</code>、<code>enhancements.staleAnnotations</code>。
     *
     * @param identity 身份；类型为 <code>String</code>
     * @param request 本次操作的请求参数对象；类型为 <code>Request</code>
     * @param historical historical；类型为 <code>boolean</code>
     * @return 类型为 <code>Article</code> 的处理结果
     */
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
        changed(a.getId());
        return get(identity,true);
    }
    /**
     * 设置已发布。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 关键处理步骤调用 <code>setPublished</code>。
     *
     * @param identity 身份；类型为 <code>String</code>
     * @param publish 发布；类型为 <code>boolean</code>
     * @return 类型为 <code>Article</code> 的处理结果
     */
    @Override @Transactional public Article setPublished(String identity,boolean publish) { return setPublished(identity,publish,null); }
    /**
     * 设置已发布。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.lockById</code>、<code>mapper.rights</code>、<code>mapper.setStatus</code>。
     *
     * @param identity 身份；类型为 <code>String</code>
     * @param publish 发布；类型为 <code>boolean</code>
     * @param expectedVersion expected版本；类型为 <code>Long</code>
     * @return 类型为 <code>Article</code> 的处理结果
     */
    @Override @Transactional public Article setPublished(String identity,boolean publish,Long expectedVersion) {
        Article a=mapper.lockById(id(identity));if(a==null) notFound();a.setRights(mapper.rights(a.getId()));
        if(expectedVersion!=null && !expectedVersion.equals(a.getRowVersion())) conflict();
        if(publish) requirePublic(a);
        if(mapper.setStatus(a.getId(),publish?"PUBLISHED":"WITHDRAWN",a.getRowVersion())!=1) conflict();changed(a.getId());return get(identity,true);
    }
    /**
     * 删除阅读。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.lockById</code>、<code>mapper.delete</code>。
     *
     * @param identity 身份；类型为 <code>String</code>
     */
    @Override @Transactional public void delete(String identity) { Article a=mapper.lockById(id(identity));if(a==null) notFound();if(mapper.delete(a.getId(),a.getRowVersion())!=1) conflict();changed(a.getId()); }
    /**
     * changed处理入口。
     * 主要协作调用：<code>events.publishEvent</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     */
    private void changed(Long id) { events.publishEvent(new EnglishSearchContentChangedEvent("ENGLISH_READING",id)); }
    /**
     * 从输入构造请求。
     * 关键处理步骤调用 <code>a.setTitle</code>、<code>EnglishBodyValidator.title</code>、<code>r.getTitle</code>、<code>a.setSummary</code>。
     *
     * @param r r；类型为 <code>Request</code>
     * @param old old；类型为 <code>Article</code>
     * @return 类型为 <code>Article</code> 的处理结果
     */
    private Article fromRequest(Request r,Article old) {
        Article a=new Article(); a.setTitle(EnglishBodyValidator.title(r.getTitle()));a.setSummary(EnglishBodyValidator.blank(r.getSummary()));a.setBodyMarkdown(EnglishBodyValidator.blank(r.getBodyMarkdown()));
        a.setTranslationZhMarkdown(r.getTranslationZhMarkdown());a.setPrimaryTopicId(r.getPrimaryTopicId());
        a.setSourceName(r.getSourceName());a.setSourceUrl(r.getSourceUrl());a.setSortOrder(r.getSortOrder()==null?0:r.getSortOrder());a.setContentOrigin(r.getContentOrigin()==null?(old==null?"EXTERNAL":old.getContentOrigin()):r.getContentOrigin());
        Rights rights=r.getRights();if(rights==null) rights=old==null?new Rights():old.getRights();if(rights==null) rights=new Rights();if(rights.getRightsStatus()==null) rights.setRightsStatus("PENDING");
        if(old!=null && r.getRights()==null && (!Objects.equals(old.getBodyMarkdown(),a.getBodyMarkdown()) || !Objects.equals(old.getSourceUrl(),a.getSourceUrl()))) rights.setRightsStatus("PENDING");
        a.setRights(rights);return a;
    }
    /**
     * 校验阅读。
     * 关键处理步骤调用 <code>validate</code>。
     *
     * @param r r；类型为 <code>Request</code>
     */
    private void validate(Request r) { validate(r,false); }
    /**
     * 校验阅读。
     * 主要协作调用：<code>metadata.validateSnapshot</code>、<code>metadata.validate</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     *
     * @param r r；类型为 <code>Request</code>
     * @param historical historical；类型为 <code>boolean</code>
     */
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
    /**
     * 检查所需公开。
     * 关键处理步骤调用 <code>EnglishBodyValidator.requireBody</code>、<code>a.getBodyMarkdown</code>、<code>a.getRights</code>、<code>uncleared</code>。
     *
     * @param a a；类型为 <code>Article</code>
     */
    public static void requirePublic(Article a) {
        EnglishBodyValidator.requireBody(a.getBodyMarkdown());
        if(a.getRights()!=null && "BLOCKED".equals(a.getRights().getRightsStatus())) uncleared();
        if(!"ORIGINAL".equals(a.getContentOrigin()) && (a.getRights()==null || !"CLEARED".equals(a.getRights().getRightsStatus()) || a.getRights().getRightsBasis()==null || a.getRights().getRightsBasis().isBlank())) uncleared();
    }
    /**
     * uncleared处理入口。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     */
    private static void uncleared() { throw new ApiException("READING_RIGHTS_UNCLEARED","公开外部作品前请核查权利依据",409); }
    /**
     * 冲突处理入口。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     */
    private static void conflict() { throw new ApiException("READING_VERSION_CONFLICT","内容已变化，请刷新后再编辑",409); }
    /**
     * 不存在处理入口。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     */
    private static void notFound() { throw new ApiException("ENGLISH_DOCUMENT_NOT_FOUND","文章不存在或未公开",404); }
    /**
     * 当前业务对象的数据库标识处理入口。
     * 关键处理步骤调用 <code>Long.parseLong</code>。
     * 实现中的显式失败类型包括 <code>NumberFormatException</code>、<code>ApiException</code>；具体触发条件见对应分支。
     *
     * @param value 值；类型为 <code>String</code>
     * @return 类型为 <code>long</code> 的处理结果
     */
    public static long id(String value) { try { long id=Long.parseLong(value);if(id<=0) throw new NumberFormatException();return id; } catch(NumberFormatException e) { throw new ApiException("ENGLISH_ID_INVALID","无效的内容编号",400); } }
}
