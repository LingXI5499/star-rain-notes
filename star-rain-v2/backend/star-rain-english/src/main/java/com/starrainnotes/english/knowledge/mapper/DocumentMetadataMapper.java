package com.starrainnotes.english.knowledge.mapper;
import com.starrainnotes.english.taxonomy.dto.TaxonomyDto.Node;
import com.starrainnotes.english.knowledge.dto.RevisionDto.Revision;
import java.util.List;
import org.apache.ibatis.annotations.Param;
public interface DocumentMetadataMapper {
    List<Node> tags(@Param("kind") String kind,@Param("id") long id);
    void clearTags(@Param("kind") String kind,@Param("id") long id);
    void insertTags(@Param("kind") String kind,@Param("id") long id,@Param("ids") List<Long> ids);
    long revisionCount(@Param("kind") String kind,@Param("id") long id);
    long nextRevision(@Param("kind") String kind,@Param("id") long id);
    List<Revision> revisions(@Param("kind") String kind,@Param("id") long id,@Param("offset") long offset,@Param("limit") int limit);
    Revision revision(@Param("kind") String kind,@Param("id") long id,@Param("no") long no);
    void insertRevision(@Param("kind") String kind,@Param("id") long id,@Param("no") long no,@Param("actor") long actor,@Param("snapshot") String snapshot,@Param("note") String note);
}
