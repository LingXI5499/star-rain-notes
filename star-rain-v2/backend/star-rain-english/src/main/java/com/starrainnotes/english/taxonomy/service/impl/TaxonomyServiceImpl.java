package com.starrainnotes.english.taxonomy.service.impl;
import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.english.taxonomy.dto.TaxonomyDto.Node;
import com.starrainnotes.english.taxonomy.mapper.TaxonomyMapper;
import com.starrainnotes.english.taxonomy.service.TaxonomyService;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor
public class TaxonomyServiceImpl implements TaxonomyService {
    private final TaxonomyMapper mapper;
    @Override @Transactional(readOnly=true) public List<Node> tree(boolean admin) {
        Map<Long,Node> nodes=new LinkedHashMap<>();
        mapper.list(admin).forEach(n->{ n.setChildren(new ArrayList<>()); nodes.put(n.getId(),n); });
        List<Node> roots=new ArrayList<>();
        nodes.values().forEach(n->{ if(n.getParentId()==null) roots.add(n); else if(nodes.containsKey(n.getParentId())) nodes.get(n.getParentId()).getChildren().add(n); });
        return roots;
    }
    @Override public Node get(String slug) {
        return tree(false).stream().flatMap(n->java.util.stream.Stream.concat(java.util.stream.Stream.of(n),n.getChildren().stream()))
            .filter(n->n.getSlug().equals(slug)).findFirst().orElseThrow(()->new ApiException("ENGLISH_TAXONOMY_NOT_FOUND","分类不存在",404));
    }
    @Override public List<Long> validateIds(List<Long> ids,String dimension) { return validateIds(ids,dimension,false); }
    @Override public List<Long> validateSnapshotIds(List<Long> ids,String dimension) { return validateIds(ids,dimension,true); }
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
    @Override public List<Long> descendants(Long id,String dimension) {
        if(id==null) return List.of(); validateIds(List.of(id),dimension);
        return mapper.list(false).stream().filter(n->id.equals(n.getId()) || id.equals(n.getParentId())).map(Node::getId).toList();
    }
    @Override @Transactional public Node create(Node request) {
        validate(request,null); request.setId(null); request.setEnabled(true); request.setWorldBaseline(false);
        if(mapper.bySlug(request.getSlug())!=null) throw new ApiException("ENGLISH_TAXONOMY_CONFLICT","分类地址已存在",409);
        mapper.insert(request); return mapper.byId(request.getId());
    }
    @Override @Transactional public Node update(long id,Node request) {
        Node old=mapper.byId(id); if(old==null) invalid(); validate(request,old);
        request.setId(id); request.setSlug(old.getSlug()); request.setDimension(old.getDimension()); request.setParentId(old.getParentId());
        mapper.update(request); return mapper.byId(id);
    }
    @Override @Transactional public void disable(long id) { if(mapper.byId(id)==null) invalid(); mapper.disable(id); }
    private void validate(Node n,Node old) {
        if(n==null || n.getDimension()==null || !List.of("TOPIC","GENRE","PURPOSE").contains(n.getDimension()) || n.getName()==null || n.getName().isBlank() || n.getName().length()>100
          || n.getNameEn()==null || n.getNameEn().isBlank() || n.getNameEn().length()>150 || n.getSlug()==null || !n.getSlug().matches("[a-z0-9][a-z0-9-]{0,199}") || (n.getDescription()!=null && n.getDescription().length()>500)) invalid();
        if(old!=null && (!old.getSlug().equals(n.getSlug()) || !old.getDimension().equals(n.getDimension()) || !Objects.equals(old.getParentId(),n.getParentId()))) invalid();
        if(n.getParentId()!=null) {
            Node p=mapper.byId(n.getParentId());
            if(p==null || !p.isEnabled() || p.getParentId()!=null || !p.getDimension().equals(n.getDimension()) || "PURPOSE".equals(n.getDimension())) invalid();
        }
    }
    private static void invalid() { throw new ApiException("ENGLISH_TAXONOMY_INVALID","分类不存在、层级或维度不匹配",400); }
}
