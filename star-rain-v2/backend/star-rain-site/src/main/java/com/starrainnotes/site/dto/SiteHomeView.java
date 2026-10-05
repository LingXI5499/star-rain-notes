package com.starrainnotes.site.dto;

import com.starrainnotes.site.api.dto.SitePublicConfig;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SiteHomeView {
    private SitePublicConfig config;
    private List<HomeSectionData> sections;
}
