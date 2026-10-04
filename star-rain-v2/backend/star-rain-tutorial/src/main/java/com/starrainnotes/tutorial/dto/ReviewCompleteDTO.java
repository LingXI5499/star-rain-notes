package com.starrainnotes.tutorial.dto;

import com.starrainnotes.tutorial.enumeration.RecallRating;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewCompleteDTO {
    @NotNull
    private RecallRating rating;
}
