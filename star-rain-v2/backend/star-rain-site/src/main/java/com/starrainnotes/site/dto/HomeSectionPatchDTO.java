package com.starrainnotes.site.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class HomeSectionPatchDTO {
    private String displayName;
    private Boolean enabled;
    private JsonNode config;
}
