package com.starrainnotes.seo.source;

import com.starrainnotes.profile.api.ProfilePublicApi;
import com.starrainnotes.profile.vo.ProfileVO;
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
        if (!supports(path) || profiles.publicProfileId() == null) return Optional.empty();
        ProfileVO profile = profiles.summary();
        StringBuilder body = new StringBuilder(profile.getBioMarkdown() == null ? "" : profile.getBioMarkdown());
        if (profile.getExperiences() != null) profile.getExperiences().forEach(item -> {
            body.append('\n').append(item.getTitle());
            if (item.getOrganization() != null) body.append(' ').append(item.getOrganization());
            if (item.getDescriptionMd() != null) body.append(' ').append(item.getDescriptionMd());
        });
        if (profile.getSkills() != null) profile.getSkills().forEach(item -> body.append('\n').append(item.getName()));
        return Optional.of(SeoSourceDocument.builder().routePath("/about").contentType("PROFILE")
            .contentId(profiles.publicProfileId()).title(profile.getDisplayName())
            .summary(profile.getHeadline()).bodyMarkdown(body.toString()).build());
    }

    @Override
    public List<SeoSourceDocument> listPublished() { return loadByRoute("/about").stream().toList(); }
}
