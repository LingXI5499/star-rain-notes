package com.starrainnotes.portfolio.vo;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WorkTemplateVO {
    private String id;
    private String code;
    private String name;
    private String description;
    private JsonNode sections;
}
