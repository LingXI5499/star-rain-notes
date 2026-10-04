package com.starrainnotes.profile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.starrainnotes.blog.api.BlogReferenceApi;
import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.media.api.MediaAssetApi;
import com.starrainnotes.media.api.MediaReferenceApi;
import com.starrainnotes.media.api.dto.MediaAssetSummary;
import com.starrainnotes.portfolio.api.PortfolioReferenceApi;
import com.starrainnotes.profile.dto.ExperienceDTO;
import com.starrainnotes.profile.dto.FeaturedContentDTO;
import com.starrainnotes.profile.dto.SocialLinkDTO;
import com.starrainnotes.profile.entity.ExperienceEntity;
import com.starrainnotes.profile.entity.FeaturedContentEntity;
import com.starrainnotes.profile.entity.ProfileEntity;
import com.starrainnotes.profile.entity.SocialLinkEntity;
import com.starrainnotes.profile.event.ProfileEventPublisher;
import com.starrainnotes.profile.mapper.ExperienceMapper;
import com.starrainnotes.profile.mapper.FeaturedContentMapper;
import com.starrainnotes.profile.mapper.ProfileMapper;
import com.starrainnotes.profile.mapper.SkillMapper;
import com.starrainnotes.profile.mapper.SocialLinkMapper;
import com.starrainnotes.profile.service.impl.ProfileServiceImpl;
import com.starrainnotes.tutorial.content.api.TutorialReferenceApi;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {
    @Mock private ProfileMapper profiles;
    @Mock private ExperienceMapper experiences;
    @Mock private SkillMapper skills;
    @Mock private SocialLinkMapper socials;
    @Mock private FeaturedContentMapper featured;
    @Mock private MediaAssetApi media;
    @Mock private MediaReferenceApi mediaReferences;
    @Mock private TutorialReferenceApi tutorials;
    @Mock private BlogReferenceApi blogs;
    @Mock private PortfolioReferenceApi works;
    @Mock private ProfileEventPublisher events;
    @InjectMocks private ProfileServiceImpl service;

    @BeforeEach
    void owner() {
        ProfileEntity row = new ProfileEntity();
        row.setId(1L);
        row.setProfileKey("OWNER");
        row.setStatus("PUBLIC");
        when(profiles.selectOne(any(LambdaQueryWrapper.class))).thenReturn(row);
    }

    @Test
    void currentExperienceCannotHaveEndDate() {
        ExperienceDTO request = new ExperienceDTO();
        request.setExperienceType("CAREER");
        request.setTitle("工作");
        request.setIsCurrent(true);
        request.setEndDate(LocalDate.of(2025, 1, 1));

        ApiException error = assertThrows(ApiException.class, () -> service.saveExperience(null, request));

        assertEquals("PROFILE_DATE_INVALID", error.getCode());
        verify(experiences, never()).insert(any(ExperienceEntity.class));
    }

    @Test
    void protectedResumeCannotBeExposed() {
        MediaAssetSummary asset = new MediaAssetSummary();
        asset.setAccessLevel("PROTECTED");
        asset.setMediaType("DOCUMENT");
        asset.setMimeType("application/pdf");
        when(media.get(5L)).thenReturn(asset);

        ApiException error = assertThrows(ApiException.class, () -> service.setMedia("resume", 5L));

        assertEquals("PROFILE_MEDIA_NOT_PUBLIC", error.getCode());
        verify(profiles, never()).updateById(any(ProfileEntity.class));
    }

    @Test
    void unsafeSocialUrlIsRejected() {
        SocialLinkDTO request = new SocialLinkDTO();
        request.setPlatformCode("OTHER");
        request.setLabel("链接");
        request.setUrl("javascript:alert(1)");

        ApiException error = assertThrows(ApiException.class, () -> service.saveSocial(null, request));

        assertEquals("PROFILE_SOCIAL_URL_INVALID", error.getCode());
        verify(socials, never()).insert(any(SocialLinkEntity.class));
    }

    @Test
    void draftTutorialCannotBeFeatured() {
        FeaturedContentDTO request = new FeaturedContentDTO();
        request.setContentType("TUTORIAL");
        request.setContentId(9L);
        when(tutorials.exists(9L)).thenReturn(true);
        when(tutorials.publishedTutorial(9L)).thenReturn(Optional.empty());

        ApiException error = assertThrows(ApiException.class, () -> service.addFeatured(request));

        assertEquals("PROFILE_FEATURED_TARGET_NOT_PUBLISHED", error.getCode());
        verify(featured, never()).insert(any(FeaturedContentEntity.class));
    }
}
