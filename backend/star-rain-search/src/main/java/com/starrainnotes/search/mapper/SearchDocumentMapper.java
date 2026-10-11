package com.starrainnotes.search.mapper;

import com.starrainnotes.search.api.dto.SearchableDocument;
import com.starrainnotes.search.vo.SearchHitVO;
import com.starrainnotes.search.vo.SearchSuggestionVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SearchDocumentMapper {
    int upsert(SearchableDocument document);
    int remove(@Param("documentKey") String documentKey);
    int removeByContent(@Param("contentType") String contentType, @Param("contentId") Long contentId);
    int removeRoutePrefix(@Param("prefix") String prefix);
    List<String> activeKeys(@Param("contentType") String contentType);
    long activeCount();
    long count(@Param("query") String query, @Param("types") List<String> types);
    List<SearchHitVO> search(@Param("query") String query, @Param("types") List<String> types,
                             @Param("offset") long offset, @Param("limit") int limit);
    List<SearchSuggestionVO> suggestions(@Param("query") String query, @Param("limit") int limit);
}
