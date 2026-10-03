package com.starrainnotes.tutorial.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorialChapterVO {
    private String id;
    private String tutorialId;
    private String groupId;
    private String slug;
    private String title;
    private String summary;
    private String bodyMarkdown;
    private Integer sortOrder;
    private String status;
    private LocalDateTime updatedAt;
}
