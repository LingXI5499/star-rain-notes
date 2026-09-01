package com.starrainnotes.vocabulary.dto;

import jakarta.validation.constraints.Min;

/**
 * Admin correction of the manual memory counter.
 */
public record SetMemoryRequest(
        @Min(0) int memoryCount) {
}
