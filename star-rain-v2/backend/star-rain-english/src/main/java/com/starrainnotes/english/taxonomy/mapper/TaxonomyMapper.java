package com.starrainnotes.english.taxonomy.mapper;
import com.starrainnotes.english.taxonomy.dto.TaxonomyDto.Node;
import java.util.List;
import org.apache.ibatis.annotations.Param;
public interface TaxonomyMapper {
    List<Node> list(@Param("admin") boolean admin);
    Node byId(@Param("id") long id);
    Node bySlug(@Param("slug") String slug);
    int insert(Node node);
    int update(Node node);
    int disable(@Param("id") long id);
}
