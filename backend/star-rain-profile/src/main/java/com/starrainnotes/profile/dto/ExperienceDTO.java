package com.starrainnotes.profile.dto;

import java.time.LocalDate;
import lombok.Data;

@Data
public class ExperienceDTO {
    private String experienceType;
    private String title;
    private String organization;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isCurrent;
    private String descriptionMd;
}
