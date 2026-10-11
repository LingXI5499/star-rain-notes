package com.starrainnotes.tutorial.learning.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningTutorialProgressVO {
    private String tutorialId;
    private int totalChapters;
    private int completedChapters;
    private List<LearningProgressVO> chapters;
}
