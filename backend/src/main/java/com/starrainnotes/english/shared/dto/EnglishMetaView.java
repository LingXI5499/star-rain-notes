package com.starrainnotes.english.shared.dto;

import com.starrainnotes.english.shared.cefr.dto.CefrLevelView;
import com.starrainnotes.english.shared.taxonomy.dto.TaxonomyTermView;

import java.util.List;
import java.util.Map;

/**
 * Public English meta dictionary (方案 §10.2): the unified taxonomy tree, the
 * CEFR ladder, and the per-module question-type codes. Public pages use it to
 * render pickers without hard-coding business values.
 */
public record EnglishMetaView(
        List<TaxonomyTermView> taxonomy,
        List<CefrLevelView> cefr,
        Map<String, List<String>> questionTypes) {
}
