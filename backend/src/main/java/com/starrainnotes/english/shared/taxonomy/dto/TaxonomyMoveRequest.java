package com.starrainnotes.english.shared.taxonomy.dto;

import jakarta.validation.constraints.Min;

public record TaxonomyMoveRequest(@Min(0) Integer targetIndex) {
}
