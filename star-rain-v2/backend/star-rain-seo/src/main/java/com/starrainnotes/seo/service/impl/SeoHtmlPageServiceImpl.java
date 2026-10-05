package com.starrainnotes.seo.service.impl;

import com.starrainnotes.seo.api.SeoPageApi;
import com.starrainnotes.seo.api.dto.SeoPageSnapshot;
import com.starrainnotes.seo.service.SeoHtmlPageService;
import com.starrainnotes.seo.service.SeoInteractiveAssetService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SeoHtmlPageServiceImpl implements SeoHtmlPageService {
    private final SeoPageApi pages;
    private final SeoInteractiveAssetService assets;

    @Override
    public Optional<String> html(String path) {
        return pages.getByRoute(path).map(SeoPageSnapshot::getHtmlSnapshot).map(assets::include);
    }
}
