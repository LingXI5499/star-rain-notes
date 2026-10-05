package com.starrainnotes.english.listening.service;

import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.english.listening.dto.ListeningSegmentDto.Request;
import com.starrainnotes.english.listening.dto.ListeningSegmentDto.Segment;
import com.starrainnotes.english.listening.mapper.ListeningSegmentMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListeningSegmentService {
    private final ListeningSegmentMapper mapper;

    @Transactional(readOnly = true)
    public List<Segment> list(String itemIdentity, boolean admin) {
        return admin ? mapper.listForAdmin(id(itemIdentity)) : mapper.listForPublic(itemIdentity);
    }

    @Transactional
    public Segment create(String itemId, Request request) {
        validate(request);
        Segment segment = fromRequest(id(itemId), request);
        mapper.insert(segment);
        return required(segment.getListeningItemId(), segment.getId());
    }

    @Transactional
    public Segment update(String itemId, String segmentId, Request request) {
        validate(request);
        Segment segment = fromRequest(id(itemId), request);
        segment.setId(id(segmentId));
        if (mapper.update(segment) == 0) notFound();
        return required(segment.getListeningItemId(), segment.getId());
    }

    @Transactional
    public void delete(String itemId, String segmentId) {
        if (mapper.delete(id(itemId), id(segmentId)) == 0) notFound();
    }

    private Segment fromRequest(long itemId, Request request) {
        Segment segment = new Segment();
        segment.setListeningItemId(itemId);
        segment.setStartMs(request.startMs());
        segment.setEndMs(request.endMs());
        segment.setTranscriptText(request.transcriptText().trim());
        segment.setTranslationText(request.translationText());
        segment.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        return segment;
    }

    private Segment required(long itemId, long segmentId) {
        Segment segment = mapper.get(itemId, segmentId);
        if (segment == null) notFound();
        return segment;
    }

    private void validate(Request request) {
        if (request == null || request.startMs() == null || request.endMs() == null
                || request.startMs() < 0 || request.endMs() <= request.startMs()
                || request.transcriptText() == null || request.transcriptText().isBlank())
            throw new ApiException("ENGLISH_SEGMENT_INVALID", "请填写有效的时间范围与英文文本", 400);
    }

    private void notFound() {
        throw new ApiException("ENGLISH_SEGMENT_NOT_FOUND", "听力片段不存在", 404);
    }

    private long id(String value) {
        try { return Long.parseLong(value); }
        catch (NumberFormatException exception) {
            throw new ApiException("ENGLISH_ID_INVALID", "无效的内容编号", 400);
        }
    }
}
