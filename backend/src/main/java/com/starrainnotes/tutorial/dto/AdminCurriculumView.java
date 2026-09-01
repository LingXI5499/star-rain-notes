package com.starrainnotes.tutorial.dto;

import java.util.List;

/** Fixed two-level curriculum: tutorial -> root groups -> direct chapters. */
public record AdminCurriculumView(
        AdminCurriculumTutorialView tutorial,
        List<AdminCurriculumGroupView> groups) {
}
