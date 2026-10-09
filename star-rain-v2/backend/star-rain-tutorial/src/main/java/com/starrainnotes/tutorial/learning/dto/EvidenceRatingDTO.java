package com.starrainnotes.tutorial.learning.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data public class EvidenceRatingDTO {
 @NotNull @Pattern(regexp="FORGOT|FUZZY|REMEMBERED") private String rating;
}
