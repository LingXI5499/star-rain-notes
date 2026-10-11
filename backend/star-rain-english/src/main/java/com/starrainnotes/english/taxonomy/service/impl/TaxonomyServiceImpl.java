/*
 * TaxonomyServiceImpl：英语学习模块的业务实现文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：ApiException、Node、TaxonomyMapper、TaxonomyService。
 * 阅读入口：tree（tree处理入口）；get（读取Taxonomy）；validateIds（校验标识集合）；validateSnapshotIds（校验快照标识集合）。
 */

package com.starrainnotes.english.taxonomy.service.impl;
import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.english.taxonomy.dto.TaxonomyDto.Node;
import com.starrainnotes.english.taxonomy.mapper.TaxonomyMapper;
import com.starrainnotes.english.taxonomy.service.TaxonomyService;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * Taxonomy业务服务实现，属于英语学习模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service @RequiredArgsConstructor
public class TaxonomyServiceImpl implements TaxonomyService {
    /**
     * 数据映射。
     * 协作类型：<code>TaxonomyMapper</code>。
     */
    private final TaxonomyMapper mapper;
    /**
     * tree处理入口。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.list</code>。
     *
     * @param admin 后台；类型为 <code>boolean</code>
     * @return 本次处理得到的条目集合
     */
    @Override @Transactional(readOnly=true) public List<Node> tree(boolean admin) {
        Map<Long,Node> nodes=new LinkedHashMap<>();
        mapper.list(admin).forEach(n->{ n.setChildren(new ArrayList<>()); nodes.put(n.getId(),n); });
        List<Node> roots=new ArrayList<>();
        nodes.values().forEach(n->{ if(n.getParentId()==null) roots.add(n); else if(nodes.containsKey(n.getParentId())) nodes.get(n.getParentId()).getChildren().add(n); });
        return roots;
    }
    /**
     * 读取Taxonomy。
     * 关键处理步骤调用 <code>tree</code>、<code>java.util.stream.Stream.concat</code>、<code>java.util.stream.Stream.of</code>、<code>n.getChildren</code>。
     *
     * @param slug 公开固定链接的路径标识；类型为 <code>String</code>
     * @return 类型为 <code>Node</code> 的处理结果
     */
    @Override public Node get(String slug) {
        return tree(false).stream().flatMap(n->java.util.stream.Stream.concat(java.util.stream.Stream.of(n),n.getChildren().stream()))
            .filter(n->n.getSlug().equals(slug)).findFirst().orElseThrow(()->new ApiException("ENGLISH_TAXONOMY_NOT_FOUND","分类不存在",404));
    }
    /**
     * 校验标识集合。
     * 关键处理步骤调用 <code>validateIds</code>。
     *
     * @param ids 标识集合；类型为 <code>List&lt;Long&gt;</code>
     * @param dimension dimension；类型为 <code>String</code>
     * @return 本次处理得到的条目集合
     */
    @Override public List<Long> validateIds(List<Long> ids,String dimension) { return validateIds(ids,dimension,false); }
    /**
     * 校验快照标识集合。
     * 关键处理步骤调用 <code>validateIds</code>。
     *
     * @param ids 标识集合；类型为 <code>List&lt;Long&gt;</code>
     * @param dimension dimension；类型为 <code>String</code>
     * @return 本次处理得到的条目集合
     */
    @Override public List<Long> validateSnapshotIds(List<Long> ids,String dimension) { return validateIds(ids,dimension,true); }
    /**
     * 校验标识集合。
     * 主要协作调用：<code>mapper.byId</code>。
     *
     * @param ids 标识集合；类型为 <code>List&lt;Long&gt;</code>
     * @param dimension dimension；类型为 <code>String</code>
     * @param historical historical；类型为 <code>boolean</code>
     * @return 本次处理得到的条目集合
     */
    private List<Long> validateIds(List<Long> ids,String dimension,boolean historical) {
        if(ids==null || ids.isEmpty()) return List.of();
        if(ids.size()>50) invalid();
        List<Long> unique=ids.stream().distinct().toList();
        for(Long id:unique) { if(id==null || id<=0) invalid(); Node n=mapper.byId(id);
            if(n==null || (!historical && !n.isEnabled()) || !dimension.equals(n.getDimension())) invalid();
            if(n.getParentId()!=null) { Node parent=mapper.byId(n.getParentId()); if(parent==null || (!historical && !parent.isEnabled())) invalid(); }
        }
        return unique;
    }
    /**
     * descendants处理入口。
     * 主要协作调用：<code>mapper.list</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param dimension dimension；类型为 <code>String</code>
     * @return 本次处理得到的条目集合
     */
    @Override public List<Long> descendants(Long id,String dimension) {
        if(id==null) return List.of(); validateIds(List.of(id),dimension);
        return mapper.list(false).stream().filter(n->id.equals(n.getId()) || id.equals(n.getParentId())).map(Node::getId).toList();
    }
    /**
     * 创建Taxonomy。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.bySlug</code>、<code>mapper.insert</code>、<code>mapper.byId</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>Node</code>
     * @return 类型为 <code>Node</code> 的处理结果
     */
    @Override @Transactional public Node create(Node request) {
        validate(request,null); request.setId(null); request.setEnabled(true); request.setWorldBaseline(false);
        if(mapper.bySlug(request.getSlug())!=null) throw new ApiException("ENGLISH_TAXONOMY_CONFLICT","分类地址已存在",409);
        mapper.insert(request); return mapper.byId(request.getId());
    }
    /**
     * 更新Taxonomy。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.byId</code>、<code>mapper.update</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>Node</code>
     * @return 类型为 <code>Node</code> 的处理结果
     */
    @Override @Transactional public Node update(long id,Node request) {
        Node old=mapper.byId(id); if(old==null) invalid(); validate(request,old);
        request.setId(id); request.setSlug(old.getSlug()); request.setDimension(old.getDimension()); request.setParentId(old.getParentId());
        mapper.update(request); return mapper.byId(id);
    }
    /**
     * 禁用Taxonomy。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>mapper.byId</code>、<code>mapper.disable</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>long</code>
     */
    @Override @Transactional public void disable(long id) { if(mapper.byId(id)==null) invalid(); mapper.disable(id); }
    /**
     * 校验Taxonomy。
     * 主要协作调用：<code>mapper.byId</code>。
     *
     * @param n n；类型为 <code>Node</code>
     * @param old old；类型为 <code>Node</code>
     */
    private void validate(Node n,Node old) {
        if(n==null || n.getDimension()==null || !List.of("TOPIC","GENRE","PURPOSE").contains(n.getDimension()) || n.getName()==null || n.getName().isBlank() || n.getName().length()>100
          || n.getNameEn()==null || n.getNameEn().isBlank() || n.getNameEn().length()>150 || n.getSlug()==null || !n.getSlug().matches("[a-z0-9][a-z0-9-]{0,199}") || (n.getDescription()!=null && n.getDescription().length()>500)) invalid();
        if(old!=null && (!old.getSlug().equals(n.getSlug()) || !old.getDimension().equals(n.getDimension()) || !Objects.equals(old.getParentId(),n.getParentId()))) invalid();
        if(n.getParentId()!=null) {
            Node p=mapper.byId(n.getParentId());
            if(p==null || !p.isEnabled() || p.getParentId()!=null || !p.getDimension().equals(n.getDimension()) || "PURPOSE".equals(n.getDimension())) invalid();
        }
    }
    /**
     * 无效处理入口。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     */
    private static void invalid() { throw new ApiException("ENGLISH_TAXONOMY_INVALID","分类不存在、层级或维度不匹配",400); }
}
