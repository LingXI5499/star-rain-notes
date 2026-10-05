package com.starrainnotes.profile.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfilePublishedDocument {
    private Long id;
    private String title;
    private String summary;
    private String bodyMarkdown;
}
