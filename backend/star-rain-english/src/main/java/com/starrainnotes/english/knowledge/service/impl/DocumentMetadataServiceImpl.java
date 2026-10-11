/*
 * DocumentMetadataServiceImpl：英语学习模块的业务实现文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：ApiException、DocumentMetadata、DocumentMetadataMapper、DocumentMetadataService、TaxonomyService。
 * 阅读入口：validate（校验文档元数据）；validateSnapshot（校验快照）；load（加载文档元数据）；replace（替换文档元数据）。
 */

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
/**
 * 文档元数据业务服务实现，属于英语学习模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service @RequiredArgsConstructor
public class DocumentMetadataServiceImpl implements DocumentMetadataService {
    /**
     * 数据映射。
     * 协作类型：<code>DocumentMetadataMapper</code>。
     */
    private final DocumentMetadataMapper mapper;
    /**
     * taxonomy。
     * 协作类型：<code>TaxonomyService</code>。
     */
    private final TaxonomyService taxonomy;
    /**
     * JSON 读写或映射协作对象。
     * 协作类型：<code>ObjectMapper</code>。
     */
    private final ObjectMapper json;
    /**
     * 校验文档元数据。
     * 关键处理步骤调用 <code>validate</code>。
     *
     * @param value 值；类型为 <code>DocumentMetadata</code>
     */
    @Override public void validate(DocumentMetadata value) { validate(value,false); }
    /**
     * 校验快照。
     * 关键处理步骤调用 <code>validate</code>。
     *
     * @param value 值；类型为 <code>DocumentMetadata</code>
     */
    @Override public void validateSnapshot(DocumentMetadata value) { validate(value,true); }
    /**
     * 标识集合处理入口。
     * 主要协作调用：<code>taxonomy.validateSnapshotIds</code>、<code>taxonomy.validateIds</code>。
     *
     * @param ids 标识集合；类型为 <code>List&lt;Long&gt;</code>
     * @param dimension dimension；类型为 <code>String</code>
     * @param historical historical；类型为 <code>boolean</code>
     * @return 本次处理得到的条目集合
     */
    private List<Long> ids(List<Long> ids,String dimension,boolean historical) { return historical?taxonomy.validateSnapshotIds(ids,dimension):taxonomy.validateIds(ids,dimension); }
    /**
     * 校验文档元数据。
     * 关键处理步骤调用 <code>value.getPrimaryTopicId</code>、<code>ids</code>、<code>List.of</code>、<code>value.setOtherTopicIds</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     *
     * @param value 值；类型为 <code>DocumentMetadata</code>
     * @param historical historical；类型为 <code>boolean</code>
     */
    private void validate(DocumentMetadata value,boolean historical) {
        if(value.getPrimaryTopicId()!=null) ids(List.of(value.getPrimaryTopicId()),"TOPIC",historical);
        value.setOtherTopicIds(ids(value.getOtherTopicIds(),"TOPIC",historical));
        value.setGenreIds(ids(value.getGenreIds(),"GENRE",historical)); value.setPurposeIds(ids(value.getPurposeIds(),"PURPOSE",historical));
        if(value.getKeywords()==null) value.setKeywords(List.of());
        if(value.getKeywords().size()>30 || value.getKeywords().stream().anyMatch(k->k==null || k.length()>100)) throw new ApiException("ENGLISH_DOCUMENT_INVALID","关键词数量或长度超出限制",400);
        value.setKeywords(value.getKeywords().stream().map(String::trim).filter(k->!k.isEmpty()).distinct().toList());
    }
    /**
     * 加载文档元数据。
     * 主要协作调用：<code>mapper.tags</code>。
     *
     * @param kind kind；类型为 <code>String</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     * @param value 值；类型为 <code>DocumentMetadata</code>
     */
    @Override public void load(String kind,long id,DocumentMetadata value) {
        var tags=mapper.tags(kind,id); value.setOtherTopicIds(tags.stream().filter(n->"TOPIC".equals(n.getDimension())).map(n->n.getId()).toList());
        value.setGenreIds(tags.stream().filter(n->"GENRE".equals(n.getDimension())).map(n->n.getId()).toList()); value.setPurposeIds(tags.stream().filter(n->"PURPOSE".equals(n.getDimension())).map(n->n.getId()).toList());
    }
    /**
     * 替换文档元数据。
     * 关键处理步骤调用 <code>replace</code>。
     *
     * @param kind kind；类型为 <code>String</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     * @param value 值；类型为 <code>DocumentMetadata</code>
     */
    @Override public void replace(String kind,long id,DocumentMetadata value) { replace(kind,id,value,false); }
    /**
     * 替换快照。
     * 关键处理步骤调用 <code>replace</code>。
     *
     * @param kind kind；类型为 <code>String</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     * @param value 值；类型为 <code>DocumentMetadata</code>
     */
    @Override public void replaceSnapshot(String kind,long id,DocumentMetadata value) { replace(kind,id,value,true); }
    /**
     * 替换文档元数据。
     * 主要协作调用：<code>mapper.clearTags</code>、<code>mapper.insertTags</code>。
     *
     * @param kind kind；类型为 <code>String</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     * @param value 值；类型为 <code>DocumentMetadata</code>
     * @param historical historical；类型为 <code>boolean</code>
     */
    private void replace(String kind,long id,DocumentMetadata value,boolean historical) {
        validate(value,historical); var ids=new LinkedHashSet<Long>(); ids.addAll(value.getOtherTopicIds()); ids.addAll(value.getGenreIds()); ids.addAll(value.getPurposeIds());
        mapper.clearTags(kind,id); if(!ids.isEmpty()) mapper.insertTags(kind,id,new ArrayList<>(ids));
    }
    /**
     * 修订集合处理入口。
     * 主要协作调用：<code>mapper.revisions</code>、<code>mapper.revisionCount</code>。
     *
     * @param kind kind；类型为 <code>String</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     * @param page 分页页码；类型为 <code>int</code>
     * @param size 大小；类型为 <code>int</code>
     * @return 类型为 <code>Page</code> 的处理结果
     */
    @Override public Page revisions(String kind,long id,int page,int size) {
        int p=Math.max(1,page),s=Math.max(1,Math.min(50,size)); return new Page(mapper.revisions(kind,id,(long)(p-1)*s,s),mapper.revisionCount(kind,id),p,s);
    }
    /**
     * 修订处理入口。
     * 主要协作调用：<code>mapper.revision</code>、<code>json.readTree</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>、<code>IllegalStateException</code>；具体触发条件见对应分支。
     *
     * @param kind kind；类型为 <code>String</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     * @param no 不含；类型为 <code>long</code>
     * @return 类型为 <code>Revision</code> 的处理结果
     */
    @Override public Revision revision(String kind,long id,long no) {
        Revision revision=mapper.revision(kind,id,no); if(revision==null) throw new ApiException("ENGLISH_REVISION_NOT_FOUND","历史版本不存在",404);
        try { revision.setSnapshot(json.readTree(revision.getSnapshotJson())); return revision; }
        catch(JsonProcessingException e) { throw new IllegalStateException("Invalid persisted revision",e); }
    }
    /**
     * 快照处理入口。
     * 主要协作调用：<code>mapper.nextRevision</code>、<code>mapper.insertRevision</code>、<code>json.writeValueAsString</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>、<code>IllegalStateException</code>；具体触发条件见对应分支。
     *
     * @param kind kind；类型为 <code>String</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     * @param actor 当前操作对应的身份对象；类型为 <code>long</code>
     * @param snapshot 快照；类型为 <code>Object</code>
     * @param note note；类型为 <code>String</code>
     * @return 类型为 <code>Revision</code> 的处理结果
     */
    @Override public Revision snapshot(String kind,long id,long actor,Object snapshot,String note) {
        if(note!=null && note.length()>500) throw new ApiException("ENGLISH_REVISION_INVALID","版本说明过长",400);
        long no=mapper.nextRevision(kind,id);
        try { mapper.insertRevision(kind,id,no,actor,json.writeValueAsString(snapshot),note); }
        catch(JsonProcessingException e) { throw new IllegalStateException("Unable to serialize revision",e); }
        return revision(kind,id,no);
    }
}
