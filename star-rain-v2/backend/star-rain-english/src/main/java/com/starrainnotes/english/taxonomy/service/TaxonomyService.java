package com.starrainnotes.english.taxonomy.service;
import com.starrainnotes.english.taxonomy.dto.TaxonomyDto.Node;
import java.util.List;
public interface TaxonomyService {
    List<Node> tree(boolean admin);
    Node get(String slug);
    List<Long> validateIds(List<Long> ids,String dimension);
    List<Long> validateSnapshotIds(List<Long> ids,String dimension);
    List<Long> descendants(Long id,String dimension);
    Node create(Node request);
    Node update(long id,Node request);
    void disable(long id);
}
