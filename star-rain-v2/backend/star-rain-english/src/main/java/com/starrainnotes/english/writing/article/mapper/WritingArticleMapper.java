package com.starrainnotes.english.writing.article.mapper;
import com.starrainnotes.english.writing.article.dto.WritingArticleDto.Article;
import java.util.List;
import org.apache.ibatis.annotations.Param;
public interface WritingArticleMapper {
    long count(@Param("owner") Long owner,@Param("search") String search,@Param("state") String state,@Param("topics") List<Long> topics,@Param("genres") List<Long> genres,@Param("purposes") List<Long> purposes);
    List<Article> list(@Param("owner") Long owner,@Param("search") String search,@Param("state") String state,@Param("topics") List<Long> topics,@Param("genres") List<Long> genres,@Param("purposes") List<Long> purposes,@Param("offset") long offset,@Param("limit") int limit);
    Article owned(@Param("id") long id,@Param("owner") long owner);
    Article locked(@Param("id") long id,@Param("owner") long owner);
    Article publicBySlug(String slug);
    void insert(Article article); void finishSlug(Article article);
    int update(Article article);
    int status(@Param("id") long id,@Param("owner") long owner,@Param("version") long version,@Param("state") String state,@Param("visibility") String visibility);
    int delete(@Param("id") long id,@Param("owner") long owner,@Param("version") long version);
}
