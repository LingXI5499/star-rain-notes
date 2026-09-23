package com.starrainnotes.english.shared.exercise.dto;

import java.util.List;

public record CheckResultView(
        int score,
        int total,
        List<CheckItemView> items) {
}
