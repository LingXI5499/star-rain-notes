package com.starrainnotes.account.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterDTO {

    @NotBlank
    @Pattern(regexp = "[A-Za-z0-9_]{3,50}")
    private String username;
    @NotBlank
    @Email
    @Size(max = 128)
    private String email;
    @NotBlank
    @Pattern(regexp = "[0-9]{6}")
    private String verificationCode;
    @NotBlank
    private String password;
    @NotBlank
    private String confirmPassword;
}
