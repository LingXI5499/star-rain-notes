package com.starrainnotes.tutorial.assembler;

import com.starrainnotes.site.service.SiteSettingsTimezone;
import com.starrainnotes.tutorial.dto.AdminTutorialDetailView;
import com.starrainnotes.tutorial.entity.Tutorial;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Explicit API mapping for tutorial persistence rows. */
@Component
@RequiredArgsConstructor
public class TutorialAssembler {
    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private final SiteSettingsTimezone timezone;

    public AdminTutorialDetailView toAdminDetail(Tutorial tutorial, String categoryName) {
        return new AdminTutorialDetailView(tutorial.getId(), tutorial.getCategoryId(), categoryName,
                tutorial.getTitle(), tutorial.getSlug(), tutorial.getSummary(), tutorial.getCoverMediaId(),
                tutorial.getPublishStatus(), tutorial.getSortOrder(), tutorial.getSeoTitle(), tutorial.getSeoDescription(),
                format(tutorial.getPublishedAt()), format(tutorial.getCreatedAt()), format(tutorial.getUpdatedAt()));
    }

    private String format(LocalDateTime utc) { return utc == null ? null : timezone.atSite(utc).format(ISO_OFFSET); }
}
