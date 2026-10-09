package com.starrainnotes.tutorial.learning.dto;
import jakarta.validation.constraints.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
@Data
public class EvidencePlanDTO {
 @NotNull private Long tutorialId;
 @NotBlank @Size(max=160) private String name;
 private boolean entireTutorial;
 @Size(max=1000) private List<@NotNull Long> groupIds = new ArrayList<>();
 @Size(max=10000) private List<@NotNull Long> chapterIds = new ArrayList<>();
 @Min(5) @Max(30) private Integer targetCardsPerTask = 12;
}
