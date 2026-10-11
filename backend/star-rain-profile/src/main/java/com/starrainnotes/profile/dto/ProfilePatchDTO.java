package com.starrainnotes.profile.dto;

import lombok.Data;

@Data
public class ProfilePatchDTO {
    private String displayName;
    private String headline;
    private String bioMarkdown;
    private String locationText;
}
