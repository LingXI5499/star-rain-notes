package com.starrainnotes.english.reading.mapper;
import com.starrainnotes.english.reading.dto.ReadingEnhancementDto.Item;
import java.util.List;
import org.apache.ibatis.annotations.Param;
public interface ReadingEnhancementMapper {
    List<Item> list(@Param("kind") String kind,@Param("articleId") long id);
    Item item(@Param("kind") String kind,@Param("articleId") long id,@Param("id") long item);
    int touch(@Param("articleId") long id,@Param("version") long version);
    void stale(@Param("articleId") long id,@Param("language") String language);
    void staleAnnotations(@Param("articleId") long id);
    void clear(@Param("kind") String kind,@Param("articleId") long id);
    void insert(@Param("kind") String kind,@Param("articleId") long id,@Param("item") Item item);
    int update(@Param("kind") String kind,@Param("articleId") long id,@Param("item") Item item);
    int delete(@Param("kind") String kind,@Param("articleId") long id,@Param("id") long item);
}
