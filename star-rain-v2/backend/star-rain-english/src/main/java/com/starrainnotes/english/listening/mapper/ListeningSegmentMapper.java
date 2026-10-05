package com.starrainnotes.english.listening.mapper;

import com.starrainnotes.english.listening.dto.ListeningSegmentDto.Segment;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ListeningSegmentMapper {
    List<Segment> listForAdmin(@Param("itemId") long itemId);
    List<Segment> listForPublic(@Param("slug") String slug);
    Segment get(@Param("itemId") long itemId, @Param("segmentId") long segmentId);
    int insert(Segment segment);
    int update(Segment segment);
    int delete(@Param("itemId") long itemId, @Param("segmentId") long segmentId);
}
