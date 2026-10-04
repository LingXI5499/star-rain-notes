package com.starrainnotes.site.home;

import com.starrainnotes.site.config.SitePublicConfig;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SiteHomeView {
    private SitePublicConfig config;
    private List<HomeSectionData> sections;
}
