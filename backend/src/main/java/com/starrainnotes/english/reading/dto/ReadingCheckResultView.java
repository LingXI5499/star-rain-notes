package com.starrainnotes.english.reading.dto;

import java.util.List;

public record ReadingCheckResultView(
        int score,
        int total,
        List<ReadingCheckItemView> items) {
}
