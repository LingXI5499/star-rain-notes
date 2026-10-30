package com.starrainnotes.english.library.mapper;

import com.starrainnotes.english.library.dto.LibraryDto.*;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/** All filters use bound parameters; taxonomy identities are validated by the service. */
public interface LibraryMapper {
    Long lockTopic(@Param("code") String code);
    Long managedTheme(@Param("layer") String layer);
    Overview overview();
    List<Count> counts(@Param("module") String module);
    long countWords(@Param("search") String search, @Param("code") String code, @Param("status") String status, @Param("theme") Long theme);
    List<Word> words(@Param("search") String search, @Param("code") String code,
                     @Param("status") String status, @Param("offset") long offset, @Param("limit") int limit, @Param("theme") Long theme);
    List<String> topicCodes(@Param("id") long id);
    Word lock(@Param("id") long id);
    int updateMapping(@Param("id") long id, @Param("value") Classification value);
    void clearTopics(@Param("id") long id);
    void addTopic(@Param("id") long id, @Param("term") long term);
    List<Essay> essays(@Param("search") String search, @Param("tag") String tag,
        @Param("month") String month, @Param("genre") String genre, @Param("offset") long offset, @Param("limit") int limit);
    long countEssays(@Param("search") String search, @Param("tag") String tag,
        @Param("month") String month, @Param("genre") String genre);
    List<Map<String, Object>> essayTags();
    List<Map<String, Object>> essayMonths();
    List<Map<String, Object>> essayGenres();
}
