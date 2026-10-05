package com.starrainnotes.seo.service;

import com.starrainnotes.seo.api.SeoPageApi;
import com.starrainnotes.seo.api.dto.SeoPageSnapshot;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SeoHtmlPageService {
    private final SeoPageApi pages;
    private final SeoInteractiveAssetService assets;

    public Optional<String> html(String path) {
        return pages.getByRoute(path).map(SeoPageSnapshot::getHtmlSnapshot).map(assets::include);
    }
}
