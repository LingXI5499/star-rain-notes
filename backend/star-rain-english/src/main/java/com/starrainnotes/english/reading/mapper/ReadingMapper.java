package com.starrainnotes.english.reading.mapper;
import com.starrainnotes.english.reading.dto.ReadingDto.Article;
import com.starrainnotes.english.reading.dto.ReadingDto.Rights;
import java.util.List;
import org.apache.ibatis.annotations.Param;
public interface ReadingMapper {
    long count(@Param("publicOnly") boolean publicOnly,@Param("search") String search,@Param("topics") List<Long> topics,@Param("genres") List<Long> genres,@Param("purposes") List<Long> purposes);
    List<Article> list(@Param("publicOnly") boolean publicOnly,@Param("search") String search,@Param("topics") List<Long> topics,@Param("genres") List<Long> genres,@Param("purposes") List<Long> purposes,@Param("offset") long offset,@Param("limit") int limit);
    Article byId(@Param("id") long id);
    Article lockById(@Param("id") long id);
    Article publicBySlug(@Param("slug") String slug);
    int insert(Article article);
    int finishSlug(Article article);
    int update(Article article);
    int setStatus(@Param("id") long id,@Param("status") String status,@Param("version") long version);
    int delete(@Param("id") long id,@Param("version") long version);
    Rights rights(@Param("id") long id);
    void saveRights(@Param("id") long id,@Param("rights") Rights rights,@Param("sourceName") String sourceName,@Param("sourceUrl") String sourceUrl);
}
