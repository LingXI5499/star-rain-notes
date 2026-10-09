package com.starrainnotes.tutorial.learning.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data public class ReviewSessionDTO {
 @NotNull @Pattern(regexp="RECOMMENDED|STABLE_AUDIT") private String mode = "RECOMMENDED";
 @NotNull @Min(1) @Max(100) private Integer count = 10;
}
