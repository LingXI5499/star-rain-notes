package com.starrainnotes.profile.dto;

import lombok.Data;

@Data
public class SocialLinkDTO {
    private String platformCode;
    private String label;
    private String url;
}
