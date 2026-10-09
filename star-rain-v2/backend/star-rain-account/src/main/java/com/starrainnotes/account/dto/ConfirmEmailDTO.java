package com.starrainnotes.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmEmailDTO {

    @NotBlank
    @Pattern(regexp = "[0-9]{6}")
    private String verificationCode;
}
