/*
 * KnowledgeServiceImpl：英语学习模块的业务实现文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：ApiException、Page、KnowledgeMapper、KnowledgeService、EnglishBodyValidator、TaxonomyService。
 * 阅读入口：list（列出知识）。
 */

package com.starrainnotes.english.knowledge.service.impl;
import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.english.knowledge.dto.KnowledgeDto.Page;
import com.starrainnotes.english.knowledge.mapper.KnowledgeMapper;
import com.starrainnotes.english.knowledge.service.KnowledgeService;
import com.starrainnotes.english.knowledge.utils.EnglishBodyValidator;
import com.starrainnotes.english.taxonomy.service.TaxonomyService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * 知识业务服务实现，属于英语学习模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service @RequiredArgsConstructor
public class KnowledgeServiceImpl implements KnowledgeService {
    /**
     * 数据映射。
     * 协作类型：<code>KnowledgeMapper</code>。
     */
    private final KnowledgeMapper mapper;
    /**
     * taxonomy。
     * 协作类型：<code>TaxonomyService</code>。
     */
    private final TaxonomyService taxonomy;
    /**
     * 列出知识。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>taxonomy.descendants</code>、<code>mapper.list</code>、<code>mapper.count</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     *
     * @param type 类型；类型为 <code>String</code>
     * @param search 搜索；类型为 <code>String</code>
     * @param topic 专题；类型为 <code>Long</code>
     * @param genre genre；类型为 <code>Long</code>
     * @param purpose purpose；类型为 <code>Long</code>
     * @param page 分页页码；类型为 <code>int</code>
     * @param size 大小；类型为 <code>int</code>
     * @return 类型为 <code>Page</code> 的处理结果
     */
    @Override @Transactional(readOnly=true) public Page list(String type,String search,Long topic,Long genre,Long purpose,int page,int size) {
        if(type!=null && !type.isBlank() && !List.of("READING","WRITING").contains(type)) throw new ApiException("ENGLISH_TYPE_INVALID","内容类型无效",400);
        String term=search==null?"":search.trim();EnglishBodyValidator.length(term,100);int p=Math.max(1,page),s=Math.max(1,Math.min(50,size));
        var topics=taxonomy.descendants(topic,"TOPIC");var genres=taxonomy.descendants(genre,"GENRE");var purposes=taxonomy.descendants(purpose,"PURPOSE");
        return new Page(mapper.list(type,term,topics,genres,purposes,(long)(p-1)*s,s),mapper.count(type,term,topics,genres,purposes),p,s);
    }
}
