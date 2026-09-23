package com.starrainnotes.english.listening.domain;

import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.listening.dto.ListeningSegmentRequest;
import com.starrainnotes.english.listening.dto.ListeningSegmentView;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;

/** Validates listening segment ranges and complete timeline replacements. */
@Component
public class SegmentTimelinePolicy {
    public void validateRange(Integer start, Integer end, Integer durationSeconds) {
        if (start == null || start < 0) throw invalid("start_ms 必须为非负");
        if (end == null || end <= start) throw invalid("end_ms 必须大于 start_ms");
        if (durationSeconds != null && durationSeconds > 0 && end > durationSeconds * 1000) {
            throw invalid("end_ms 不能超过音频时长");
        }
    }

    public void validateReplacement(List<ListeningSegmentRequest> segments, Integer durationSeconds) {
        int previousEnd = -1;
        for (ListeningSegmentRequest segment : segments) {
            validateRange(segment.startMs(), segment.endMs(), durationSeconds);
            if (segment.transcriptText() == null || segment.transcriptText().isBlank()) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_LISTENING_SEGMENT_TEXT_REQUIRED",
                        "Segment transcript required", "Each listening segment must contain transcript text.");
            }
            if (previousEnd > segment.startMs()) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_LISTENING_SEGMENT_OVERLAP",
                        "Segments overlap", "Listening segments must be ordered and cannot overlap.");
            }
            previousEnd = segment.endMs();
        }
    }

    public boolean isValid(List<ListeningSegmentView> segments, Integer durationSeconds) {
        if (segments.isEmpty()) return false;
        int previousEnd = -1;
        for (ListeningSegmentView segment : segments) {
            if (segment.transcriptText() == null || segment.transcriptText().isBlank()
                    || segment.startMs() < 0 || segment.endMs() <= segment.startMs()) return false;
            if (previousEnd > segment.startMs()) return false;
            if (durationSeconds != null && durationSeconds > 0 && segment.endMs() > durationSeconds * 1000) return false;
            previousEnd = segment.endMs();
        }
        return true;
    }

    private ApiException invalid(String detail) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ENGLISH_LISTENING_SEGMENT_INVALID",
                "Invalid segment", detail);
    }
}
