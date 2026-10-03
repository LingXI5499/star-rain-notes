package com.starrainnotes.tutorial.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorialGroupVO {
    private String id;
    private String tutorialId;
    private String title;
    private String description;
    private Integer sortOrder;
    private String status;
    private long chapterCount;
    private List<TutorialChapterVO> chapters;
}
