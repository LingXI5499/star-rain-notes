/*
 * ReadingRevisionServiceImpl：英语学习模块的业务实现文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：CurrentActorApi、ApiException、DocumentMetadataService、Article、Snapshot、ReadingMapper。
 * 阅读入口：list（列出阅读修订）；get（读取阅读修订）；snapshot（快照处理入口）；restore（恢复阅读修订）。
 */

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
/**
 * 阅读修订业务服务实现，属于英语学习模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service @RequiredArgsConstructor
public class ReadingRevisionServiceImpl implements ReadingRevisionService {
    /**
     * 阅读。
     * 协作类型：<code>ReadingService</code>。
     */
    private final ReadingService reading;
    /**
     * 数据映射。
     * 协作类型：<code>ReadingMapper</code>。
     */
    private final ReadingMapper mapper;
    /**
     * enhancements。
     * 协作类型：<code>ReadingEnhancementService</code>。
     */
    private final ReadingEnhancementService enhancements;
    /**
     * 已通过元数据校验的上传信息。
     * 协作类型：<code>DocumentMetadataService</code>。
     */
    private final DocumentMetadataService metadata;
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
     * 列出阅读修订。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>reading.get</code>、<code>metadata.revisions</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param page 分页页码；类型为 <code>int</code>
     * @param size 大小；类型为 <code>int</code>
     * @return 类型为 <code>Page</code> 的处理结果
     */
    @Override @Transactional(readOnly=true) public Page list(String id,int page,int size) { Article a=reading.get(id,true);return metadata.revisions("READING",a.getId(),page,size); }
    /**
     * 读取阅读修订。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>reading.get</code>、<code>metadata.revision</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param no 不含；类型为 <code>long</code>
     * @return 类型为 <code>Revision</code> 的处理结果
     */
    @Override @Transactional(readOnly=true) public Revision get(String id,long no) { Article a=reading.get(id,true);return metadata.revision("READING",a.getId(),no); }
    /**
     * 快照处理入口。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 关键处理步骤调用 <code>lock</code>、<code>capture</code>、<code>request.getChangeNote</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param request 本次操作的请求参数对象；类型为 <code>Request</code>
     * @return 类型为 <code>Revision</code> 的处理结果
     */
    @Override @Transactional public Revision snapshot(String id,Request request) { lock(id,request);return capture(id,request.getChangeNote()); }
    /**
     * 恢复阅读修订。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>json.convertValue</code>、<code>metadata.revision</code>、<code>reading.restoreSnapshot</code>、<code>enhancements.restore</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param no 不含；类型为 <code>long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>Request</code>
     * @return 类型为 <code>Article</code> 的处理结果
     */
    @Override @Transactional public Article restore(String id,long no,Request request) {
        lock(id,request);Snapshot saved=json.convertValue(metadata.revision("READING",ReadingServiceImpl.id(id),no).getSnapshot(),Snapshot.class);
        var update=json.convertValue(saved.article(),com.starrainnotes.english.reading.dto.ReadingDto.Request.class);
        update.setRowVersion(request.getRowVersion());Article article=reading.restoreSnapshot(id,update);
        enhancements.restore(article.getId(),saved);capture(id,"恢复版本 "+no);return article;
    }
    /**
     * 锁处理入口。
     * 主要协作调用：<code>mapper.lockById</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param request 本次操作的请求参数对象；类型为 <code>Request</code>
     */
    private void lock(String id,Request request) { Article a=mapper.lockById(ReadingServiceImpl.id(id));
        if(a==null) throw new ApiException("ENGLISH_DOCUMENT_NOT_FOUND","文章不存在",404);
        if(request==null || request.getRowVersion()==null || !request.getRowVersion().equals(a.getRowVersion())) throw new ApiException("READING_VERSION_CONFLICT","内容已变化，请刷新后再编辑",409);
    }
    /**
     * capture处理入口。
     * 主要协作调用：<code>reading.get</code>、<code>enhancements.list</code>、<code>metadata.snapshot</code>、<code>actor.current</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>String</code>
     * @param note note；类型为 <code>String</code>
     * @return 类型为 <code>Revision</code> 的处理结果
     */
    private Revision capture(String id,String note) {
        Article a=reading.get(id,true);Snapshot snapshot=new Snapshot(a,enhancements.list(id,true,"alignments"),enhancements.list(id,true,"annotations"),enhancements.list(id,true,"vocabulary"));
        return metadata.snapshot("READING",a.getId(),actor.current().getAccountId(),snapshot,note);
    }
}
