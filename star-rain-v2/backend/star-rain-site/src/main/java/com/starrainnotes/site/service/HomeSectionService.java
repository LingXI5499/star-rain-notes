package com.starrainnotes.site.service;

import com.starrainnotes.site.dto.HomeSectionOrderDTO;
import com.starrainnotes.site.dto.HomeSectionPatchDTO;
import com.starrainnotes.site.entity.HomeSectionEntity;
import java.util.List;

/** Manages the configured order and visibility of homepage sections. */
public interface HomeSectionService {
    /** Returns all configured sections in display order. */
    List<HomeSectionEntity> all();

    /** Returns visible sections in display order. */
    List<HomeSectionEntity> enabled();

    /** Replaces the full section order; rejects missing or duplicate codes. */
    List<HomeSectionEntity> reorder(HomeSectionOrderDTO request);

    /** Updates one section's display name, visibility, or layout options. */
    HomeSectionEntity patch(String code, HomeSectionPatchDTO request);
}
