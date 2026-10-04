package com.starrainnotes.tutorial.learning.dto;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MasterySelfRatingDTO {
    @Pattern(regexp = "L[1-4]")
    private String level;
}
