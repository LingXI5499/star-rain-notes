/*
 * ProfileServiceTest：作者资料模块的测试文件。
 * 业务范围：个人介绍、社交链接、经历技能和精选内容。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：BlogReferenceApi、ApiException、MediaAssetApi、MediaReferenceApi、MediaAssetSummary、PortfolioReferenceApi、ExperienceDTO。
 * 阅读入口：owner（owner处理入口）；currentExperienceCannotHaveEndDate（当前ExperienceCannotHave结束日期处理入口）；protectedResumeCannotBeExposed（受保护ResumeCannotBeExposed处理入口）；unsafeSocialUrlIsRejected（不安全Social地址是否已拒绝处理入口）。
 */

package com.starrainnotes.profile;

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

/**
 * 资料业务服务测试，属于作者资料模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {
    /**
     * profiles。
     * 协作类型：<code>ProfileMapper</code>。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock private ProfileMapper profiles;
    /**
     * experiences。
     * 协作类型：<code>ExperienceMapper</code>。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock private ExperienceMapper experiences;
    /**
     * skills。
     * 协作类型：<code>SkillMapper</code>。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock private SkillMapper skills;
    /**
     * socials。
     * 协作类型：<code>SocialLinkMapper</code>。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock private SocialLinkMapper socials;
    /**
     * 精选。
     * 协作类型：<code>FeaturedContentMapper</code>。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock private FeaturedContentMapper featured;
    /**
     * 媒体。
     * 协作类型：<code>MediaAssetApi</code>。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock private MediaAssetApi media;
    /**
     * 媒体References。
     * 协作类型：<code>MediaReferenceApi</code>。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock private MediaReferenceApi mediaReferences;
    /**
     * tutorials。
     * 协作类型：<code>TutorialReferenceApi</code>。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock private TutorialReferenceApi tutorials;
    /**
     * blogs。
     * 协作类型：<code>BlogReferenceApi</code>。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock private BlogReferenceApi blogs;
    /**
     * 作品列表。
     * 协作类型：<code>PortfolioReferenceApi</code>。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock private PortfolioReferenceApi works;
    /**
     * 事件集合。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @Mock private ProfileEventPublisher events;
    /**
     * 业务服务。
     * 此字段为测试替身，不连接实际业务实现。
     */
    @InjectMocks private ProfileServiceImpl service;

    /**
     * owner处理入口。
     * 主要协作调用：<code>profiles.selectOwner</code>。
     */
    @BeforeEach
    void owner() {
        ProfileEntity row = new ProfileEntity();
        row.setId(1L);
        row.setProfileKey("OWNER");
        row.setStatus("PUBLIC");
        when(profiles.selectOwner()).thenReturn(row);
    }

    /**
     * 当前ExperienceCannotHave结束日期处理入口。
     * 主要协作调用：<code>service.saveExperience</code>。
     */
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

    /**
     * 受保护ResumeCannotBeExposed处理入口。
     * 主要协作调用：<code>media.get</code>、<code>service.setMedia</code>。
     */
    @Test
    void protectedResumeCannotBeExposed() {
        MediaAssetSummary asset = new MediaAssetSummary();
        asset.setAccessLevel("PROTECTED");
        asset.setMediaType("DOCUMENT");
        asset.setMimeType("application/pdf");
        when(media.get(5L)).thenReturn(asset);

        ApiException error = assertThrows(ApiException.class, () -> service.setMedia("resume", 5L));

        assertEquals("PROFILE_MEDIA_NOT_PUBLIC", error.getCode());
        verify(profiles, never()).updateMediaAssets(any(ProfileEntity.class));
    }

    /**
     * 不安全Social地址是否已拒绝处理入口。
     * 主要协作调用：<code>service.saveSocial</code>。
     */
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

    /**
     * 草稿教程CannotBe精选处理入口。
     * 主要协作调用：<code>tutorials.exists</code>、<code>tutorials.publishedTutorial</code>、<code>service.addFeatured</code>。
     */
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
