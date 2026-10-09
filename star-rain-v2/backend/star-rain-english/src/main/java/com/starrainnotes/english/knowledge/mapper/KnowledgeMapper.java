package com.starrainnotes.english.knowledge.mapper;
import com.starrainnotes.english.knowledge.dto.KnowledgeDto.Item;
import java.util.List;
import org.apache.ibatis.annotations.Param;
public interface KnowledgeMapper {
    long count(@Param("type") String type,@Param("search") String search,@Param("topics") List<Long> topics,@Param("genres") List<Long> genres,@Param("purposes") List<Long> purposes);
    List<Item> list(@Param("type") String type,@Param("search") String search,@Param("topics") List<Long> topics,@Param("genres") List<Long> genres,@Param("purposes") List<Long> purposes,@Param("offset") long offset,@Param("limit") int limit);
}
