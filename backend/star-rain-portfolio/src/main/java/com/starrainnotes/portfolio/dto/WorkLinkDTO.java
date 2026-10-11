package com.starrainnotes.portfolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkLinkDTO {
    @NotBlank
    @Size(max = 30)
    private String linkType;
    @NotBlank
    @Size(max = 100)
    private String label;
    @NotBlank
    @Size(max = 1000)
    private String url;
    private Integer sortOrder;
    private Boolean enabled;
}
