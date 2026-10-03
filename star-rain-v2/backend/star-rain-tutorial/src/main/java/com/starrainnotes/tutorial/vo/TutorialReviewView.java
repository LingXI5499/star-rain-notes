package com.starrainnotes.tutorial.vo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.starrainnotes.review.api.ReviewTargetView;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TutorialReviewView implements ReviewTargetView {
    private String revisionRef;
    private String title;
    private JsonNode contentSnapshot;

    @Override
    @JsonProperty("viewType")
    public String viewType() {
        return "TUTORIAL_SNAPSHOT";
    }
}
