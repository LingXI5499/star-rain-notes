package com.starrainnotes.portfolio.vo;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WorkSectionVO {
    private String id;
    private String sectionType;
    private String title;
    private String content;
    private JsonNode data;
    private Integer blockVersion;
    private Boolean visible;
    private Integer sortOrder;
    private List<SectionMediaVO> media;
}
