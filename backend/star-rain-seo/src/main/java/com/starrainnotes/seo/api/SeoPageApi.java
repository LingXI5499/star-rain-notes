package com.starrainnotes.seo.api;

import com.starrainnotes.seo.api.dto.SeoPageSnapshot;
import java.util.Optional;

public interface SeoPageApi {
    Optional<SeoPageSnapshot> getByRoute(String routePath);
    Optional<SeoPageSnapshot> getMetaByRoute(String routePath);
}
