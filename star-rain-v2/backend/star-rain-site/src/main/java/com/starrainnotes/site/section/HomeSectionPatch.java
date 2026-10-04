package com.starrainnotes.site.section;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class HomeSectionPatch {
    private String displayName;
    private Boolean enabled;
    private JsonNode config;
}
