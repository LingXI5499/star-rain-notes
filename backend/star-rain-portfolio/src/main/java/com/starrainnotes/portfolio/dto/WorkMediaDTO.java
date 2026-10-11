package com.starrainnotes.portfolio.dto;

import com.starrainnotes.portfolio.enumeration.WorkMediaUsage;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkMediaDTO {
    @NotNull
    private Long mediaAssetId;
    @NotNull
    private WorkMediaUsage usageType;
    @Size(max = 500)
    private String caption;
    private Integer sortOrder;
}
