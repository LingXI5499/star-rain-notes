package com.starrainnotes.tutorial.learning.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.starrainnotes.tutorial.learning.exception.LearningInvalidRequestException;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Set;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

/** Account queries; dates use the site's Asia/Shanghai calendar and UTC storage. */
@Data
public class LearningPageQueryDTO {
    @Min(1) private int page = 1;
    @Min(1) @Max(100) private int pageSize = 20;
    @Positive private Long tutorialId;
    @Size(max = 100) private String keyword;
    private String status;
    private String sessionType;
    private Boolean needsRevalidation;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate fromDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate toDate;

    public void validate(Set<String> statuses) {
        if (page < 1 || pageSize < 1 || pageSize > 100 || (long) (page - 1) * pageSize > Integer.MAX_VALUE
                || tutorialId != null && tutorialId <= 0 || keyword != null && keyword.length() > 100
                || status != null && !status.isBlank() && !statuses.contains(status)
                || sessionType != null && !sessionType.isBlank() && !Set.of("INITIAL_STUDY", "REVIEW", "STABLE_AUDIT").contains(sessionType)
                || fromDate != null && toDate != null && fromDate.isAfter(toDate)
                || fromDate != null && (fromDate.getYear() < 1970 || fromDate.getYear() > 9998)
                || toDate != null && (toDate.getYear() < 1970 || toDate.getYear() > 9998)) {
            throw new LearningInvalidRequestException("查询条件或分页参数无效");
        }
    }
    @JsonIgnore public int getOffset() { return (page - 1) * pageSize; }
    @JsonIgnore public LocalDateTime getFromTime() { return utc(fromDate); }
    @JsonIgnore public LocalDateTime getUntilTime() { return utc(toDate == null ? null : toDate.plusDays(1)); }
    private LocalDateTime utc(LocalDate date) {
        return date == null ? null : date.atStartOfDay(ZoneId.of("Asia/Shanghai")).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }
}
