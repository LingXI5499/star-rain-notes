package com.starrainnotes.tutorial.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewIntervalDecision {
    private int stepIndex;
    private int intervalDays;
}
