package com.starrainnotes.tutorial.content.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorialAdminVO {
    private String id;
    private String categoryId;
    private String categoryName;
    private String slug;
    private String title;
    private String summary;
    private Integer sortOrder;
    private String publicationStatus;
    private String editingStatus;
    private long chapterCount;
    private LocalDateTime publishedAt;
    private LocalDateTime updatedAt;
}
