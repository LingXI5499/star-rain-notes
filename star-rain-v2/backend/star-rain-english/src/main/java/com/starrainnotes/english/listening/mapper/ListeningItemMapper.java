package com.starrainnotes.english.listening.mapper;

import com.starrainnotes.english.listening.dto.ListeningItemDto.Item;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ListeningItemMapper {
    long count(@Param("publicOnly") boolean publicOnly, @Param("search") String search);
    List<Item> list(@Param("publicOnly") boolean publicOnly, @Param("search") String search,
                    @Param("offset") long offset, @Param("limit") int limit);
    Item byId(@Param("id") long id);
    Item publicBySlug(@Param("slug") String slug);
    int insert(Item item);
    int update(Item item);
    int setStatus(@Param("id") long id, @Param("status") String status);
    int delete(@Param("id") long id);
    int deleteSegments(@Param("itemId") long itemId);
}
