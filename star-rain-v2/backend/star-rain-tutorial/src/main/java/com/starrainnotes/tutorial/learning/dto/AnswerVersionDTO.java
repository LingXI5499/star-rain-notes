package com.starrainnotes.tutorial.learning.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data public class AnswerVersionDTO {
 @NotBlank @Size(max=100000) private String answerText;
 @NotNull @Pattern(regexp="BEFORE_REFERENCE|AFTER_REFERENCE") private String answerPhase = "BEFORE_REFERENCE";
}
