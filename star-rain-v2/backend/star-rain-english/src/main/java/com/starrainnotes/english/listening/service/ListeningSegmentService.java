package com.starrainnotes.english.listening.service;

import com.starrainnotes.english.listening.dto.ListeningSegmentDto.Request;
import com.starrainnotes.english.listening.dto.ListeningSegmentDto.Segment;
import java.util.List;

// 听力片段读写入口。
public interface ListeningSegmentService {

    List<Segment> list(String itemIdentity, boolean admin);

    Segment create(String itemId, Request request);

    Segment update(String itemId, String segmentId, Request request);

    void delete(String itemId, String segmentId);
}
