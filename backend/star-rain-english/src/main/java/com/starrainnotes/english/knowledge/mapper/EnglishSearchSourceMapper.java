package com.starrainnotes.english.knowledge.mapper;

import com.starrainnotes.english.api.dto.EnglishSearchDocument;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface EnglishSearchSourceMapper {
    List<EnglishSearchDocument> page(@Param("type") String type, @Param("afterId") Long afterId,
        @Param("limit") int limit);

    EnglishSearchDocument findPublished(@Param("type") String type, @Param("id") Long id);
}
