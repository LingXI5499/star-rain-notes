package com.starrainnotes.seo.service;

import com.starrainnotes.seo.dto.SeoSourceDocument;

import com.starrainnotes.profile.api.ProfilePublicApi;
import com.starrainnotes.profile.api.dto.ProfilePublishedDocument;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProfileSeoSourceProvider implements SeoSourceProvider {
    private final ProfilePublicApi profiles;

    @Override
    public boolean supports(String path) { return "/about".equals(path); }

    @Override
    public Optional<SeoSourceDocument> loadByRoute(String path) {
        if (!supports(path)) return Optional.empty();
        ProfilePublishedDocument profile = profiles.publishedDocument();
        if (profile == null) return Optional.empty();
        return Optional.of(SeoSourceDocument.builder().routePath("/about").contentType("PROFILE")
            .contentId(profile.getId()).title(profile.getTitle())
            .summary(profile.getSummary()).bodyMarkdown(profile.getBodyMarkdown()).build());
    }

    @Override
    public List<SeoSourceDocument> listPublished() { return loadByRoute("/about").stream().toList(); }
}
