package com.starrainnotes.profile.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.starrainnotes.blog.api.BlogReferenceApi;
import com.starrainnotes.blog.api.dto.BlogPostSummary;
import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.media.api.MediaAssetApi;
import com.starrainnotes.media.api.MediaReferenceApi;
import com.starrainnotes.media.api.dto.MediaAssetSummary;
import com.starrainnotes.media.api.dto.MediaReferenceCommand;
import com.starrainnotes.media.constant.MediaUsageCodes;
import com.starrainnotes.portfolio.api.PortfolioReferenceApi;
import com.starrainnotes.portfolio.api.dto.PortfolioPublishedWork;
import com.starrainnotes.profile.api.ProfilePublicApi;
import com.starrainnotes.profile.dto.ExperienceDTO;
import com.starrainnotes.profile.dto.FeaturedContentDTO;
import com.starrainnotes.profile.dto.ProfilePatchDTO;
import com.starrainnotes.profile.dto.SkillDTO;
import com.starrainnotes.profile.dto.SocialLinkDTO;
import com.starrainnotes.profile.entity.ExperienceEntity;
import com.starrainnotes.profile.entity.FeaturedContentEntity;
import com.starrainnotes.profile.entity.ProfileEntity;
import com.starrainnotes.profile.entity.SkillEntity;
import com.starrainnotes.profile.entity.SocialLinkEntity;
import com.starrainnotes.profile.event.ProfileEventPublisher;
import com.starrainnotes.profile.mapper.ExperienceMapper;
import com.starrainnotes.profile.mapper.FeaturedContentMapper;
import com.starrainnotes.profile.mapper.ProfileMapper;
import com.starrainnotes.profile.mapper.SkillMapper;
import com.starrainnotes.profile.mapper.SocialLinkMapper;
import com.starrainnotes.profile.service.ProfileService;
import com.starrainnotes.profile.vo.ProfileVO;
import com.starrainnotes.tutorial.content.api.PublishedTutorial;
import com.starrainnotes.tutorial.content.api.TutorialReferenceApi;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService, ProfilePublicApi {
    private static final Set<String> EXPERIENCE_TYPES = Set.of("EDUCATION", "PROJECT", "CAREER", "GROWTH", "OTHER");
    private static final Set<String> SKILL_CATEGORIES = Set.of("SKILL", "DIRECTION", "INTEREST");
    private static final Set<String> PLATFORMS = Set.of("GITHUB", "BILIBILI", "GITEE", "LINKEDIN", "EMAIL_PAGE", "OTHER");
    private final ProfileMapper profiles;
    private final ExperienceMapper experiences;
    private final SkillMapper skills;
    private final SocialLinkMapper socials;
    private final FeaturedContentMapper featured;
    private final MediaAssetApi media;
    private final MediaReferenceApi mediaReferences;
    private final TutorialReferenceApi tutorials;
    private final BlogReferenceApi blogs;
    private final PortfolioReferenceApi works;
    private final ProfileEventPublisher events;

    @Override
    @Transactional(readOnly = true)
    public ProfileVO publicProfile() {
        ProfileEntity profile = owner();
        if (!"PUBLIC".equals(profile.getStatus())) {
            throw error("PROFILE_HIDDEN", "作者资料暂未公开", 404);
        }
        return view(profile, false);
    }

    @Override
    public ProfileVO summary() {
        return publicProfile();
    }

    @Override
    @Transactional(readOnly = true)
    public Long publicProfileId() {
        ProfileEntity profile = owner();
        return "PUBLIC".equals(profile.getStatus()) ? profile.getId() : null;
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileVO adminProfile() {
        return view(owner(), true);
    }

    @Override
    @Transactional
    public ProfileVO updateBasic(ProfilePatchDTO request) {
        ProfileEntity row = owner();
        if (request == null) throw error("PROFILE_INVALID", "资料不能为空", 400);
        if (request.getDisplayName() != null) row.setDisplayName(required(request.getDisplayName(), 100, "PROFILE_INVALID", "显示名称"));
        if (request.getHeadline() != null) row.setHeadline(optional(request.getHeadline(), 255, "标题"));
        if (request.getBioMarkdown() != null) row.setBioMarkdown(optional(request.getBioMarkdown(), 30_000, "个人介绍"));
        if (request.getLocationText() != null) row.setLocationText(optional(request.getLocationText(), 120, "所在地"));
        row.setUpdatedAt(now());
        profiles.updateById(row);
        return changed(row);
    }

    @Override
    @Transactional
    public ProfileVO saveExperience(Long id, ExperienceDTO request) {
        ProfileEntity owner = owner();
        if (request == null || request.getExperienceType() == null
                || !EXPERIENCE_TYPES.contains(request.getExperienceType())) {
            throw error("PROFILE_DATE_INVALID", "经历类型无效", 400);
        }
        if (Boolean.TRUE.equals(request.getIsCurrent()) && request.getEndDate() != null
                || request.getStartDate() != null && request.getEndDate() != null
                && request.getEndDate().isBefore(request.getStartDate())) {
            throw error("PROFILE_DATE_INVALID", "经历日期范围无效", 400);
        }
        ExperienceEntity row = id == null ? new ExperienceEntity() : experience(id, owner.getId());
        row.setProfileId(owner.getId());
        row.setExperienceType(request.getExperienceType());
        row.setTitle(required(request.getTitle(), 255, "PROFILE_DATE_INVALID", "经历标题"));
        row.setOrganization(optional(request.getOrganization(), 255, "机构"));
        row.setStartDate(request.getStartDate());
        row.setEndDate(request.getEndDate());
        row.setIsCurrent(Boolean.TRUE.equals(request.getIsCurrent()));
        row.setDescriptionMd(optional(request.getDescriptionMd(), 30_000, "经历说明"));
        if (id == null) {
            row.setSortOrder(experiences.selectCount(Wrappers.<ExperienceEntity>lambdaQuery()
                    .eq(ExperienceEntity::getProfileId, owner.getId())).intValue());
            row.setStatus("ENABLED");
            row.setCreatedAt(now());
            row.setUpdatedAt(now());
            experiences.insert(row);
        } else {
            row.setUpdatedAt(now());
            experiences.updateById(row);
        }
        return changed(owner);
    }

    @Override
    @Transactional
    public ProfileVO removeExperience(Long id) {
        ProfileEntity owner = owner();
        experiences.deleteById(experience(id, owner.getId()).getId());
        return changed(owner);
    }

    @Override
    @Transactional
    public ProfileVO orderExperiences(List<Long> ids) {
        ProfileEntity owner = owner();
        List<ExperienceEntity> rows = experiences.selectList(Wrappers.<ExperienceEntity>lambdaQuery()
                .eq(ExperienceEntity::getProfileId, owner.getId()));
        checkOrder(ids, rows.stream().map(ExperienceEntity::getId).toList());
        for (ExperienceEntity row : rows) {
            row.setSortOrder(ids.indexOf(row.getId()));
            experiences.updateById(row);
        }
        return changed(owner);
    }

    @Override
    @Transactional
    public ProfileVO saveSkill(Long id, SkillDTO request) {
        ProfileEntity owner = owner();
        if (request == null || request.getCategory() == null
                || !SKILL_CATEGORIES.contains(request.getCategory())) {
            throw error("PROFILE_SKILL_CONFLICT", "技能类别无效", 400);
        }
        SkillEntity row = id == null ? new SkillEntity() : skill(id, owner.getId());
        row.setProfileId(owner.getId());
        row.setCategory(request.getCategory());
        row.setName(required(request.getName(), 120, "PROFILE_SKILL_CONFLICT", "名称"));
        row.setDescription(optional(request.getDescription(), 500, "说明"));
        row.setProficiency(optional(request.getProficiency(), 30, "熟悉程度"));
        try {
            if (id == null) {
                row.setSortOrder(skills.selectCount(Wrappers.<SkillEntity>lambdaQuery()
                        .eq(SkillEntity::getProfileId, owner.getId())).intValue());
                row.setStatus("ENABLED");
                row.setCreatedAt(now());
                row.setUpdatedAt(now());
                skills.insert(row);
            } else {
                row.setUpdatedAt(now());
                skills.updateById(row);
            }
        } catch (DuplicateKeyException exception) {
            throw error("PROFILE_SKILL_CONFLICT", "同类别下已有同名条目", 409);
        }
        return changed(owner);
    }

    @Override
    @Transactional
    public ProfileVO removeSkill(Long id) {
        ProfileEntity owner = owner();
        skills.deleteById(skill(id, owner.getId()).getId());
        return changed(owner);
    }

    @Override
    @Transactional
    public ProfileVO orderSkills(List<Long> ids) {
        ProfileEntity owner = owner();
        List<SkillEntity> rows = skills.selectList(Wrappers.<SkillEntity>lambdaQuery()
                .eq(SkillEntity::getProfileId, owner.getId()));
        checkOrder(ids, rows.stream().map(SkillEntity::getId).toList());
        for (SkillEntity row : rows) {
            row.setSortOrder(ids.indexOf(row.getId()));
            skills.updateById(row);
        }
        return changed(owner);
    }

    @Override
    @Transactional
    public ProfileVO saveSocial(Long id, SocialLinkDTO request) {
        ProfileEntity owner = owner();
        if (request == null || request.getPlatformCode() == null
                || !PLATFORMS.contains(request.getPlatformCode())) {
            throw error("PROFILE_SOCIAL_URL_INVALID", "社交平台无效", 400);
        }
        String url = safeUrl(request.getUrl());
        SocialLinkEntity row = id == null ? new SocialLinkEntity() : social(id, owner.getId());
        row.setProfileId(owner.getId());
        row.setPlatformCode(request.getPlatformCode());
        row.setLabel(required(request.getLabel(), 100, "PROFILE_SOCIAL_URL_INVALID", "链接名称"));
        row.setUrl(url);
        try {
            if (id == null) {
                row.setSortOrder(socials.selectCount(Wrappers.<SocialLinkEntity>lambdaQuery()
                        .eq(SocialLinkEntity::getProfileId, owner.getId())).intValue());
                row.setStatus("ENABLED");
                row.setCreatedAt(now());
                row.setUpdatedAt(now());
                socials.insert(row);
            } else {
                row.setUpdatedAt(now());
                socials.updateById(row);
            }
        } catch (DuplicateKeyException exception) {
            throw error("PROFILE_SOCIAL_URL_INVALID", "同一平台只能添加一条链接", 409);
        }
        return changed(owner);
    }

    @Override
    @Transactional
    public ProfileVO removeSocial(Long id) {
        ProfileEntity owner = owner();
        socials.deleteById(social(id, owner.getId()).getId());
        return changed(owner);
    }

    @Override
    @Transactional
    public ProfileVO orderSocial(List<Long> ids) {
        ProfileEntity owner = owner();
        List<SocialLinkEntity> rows = socials.selectList(Wrappers.<SocialLinkEntity>lambdaQuery()
                .eq(SocialLinkEntity::getProfileId, owner.getId()));
        checkOrder(ids, rows.stream().map(SocialLinkEntity::getId).toList());
        for (SocialLinkEntity row : rows) {
            row.setSortOrder(ids.indexOf(row.getId()));
            socials.updateById(row);
        }
        return changed(owner);
    }

    @Override
    @Transactional
    public ProfileVO setMedia(String kind, Long mediaAssetId) {
        ProfileEntity owner = owner();
        boolean avatar = "avatar".equals(kind);
        if (!avatar && !"resume".equals(kind)) throw error("PROFILE_MEDIA_NOT_PUBLIC", "媒体用途无效", 400);
        Long oldId = avatar ? owner.getAvatarMediaAssetId() : owner.getResumeMediaAssetId();
        if (oldId != null && oldId.equals(mediaAssetId)) return view(owner, true);
        if (mediaAssetId != null) {
            media.assertUsable(mediaAssetId);
            MediaAssetSummary asset = media.get(mediaAssetId);
            if (asset == null || !"PUBLIC".equals(asset.getAccessLevel())
                    || avatar && !"IMAGE".equals(asset.getMediaType())
                    || !avatar && !"application/pdf".equalsIgnoreCase(asset.getMimeType())) {
                throw error("PROFILE_MEDIA_NOT_PUBLIC", "请选择公开的图片或 PDF 文件", 400);
            }
        }
        if (avatar) owner.setAvatarMediaAssetId(mediaAssetId);
        else owner.setResumeMediaAssetId(mediaAssetId);
        owner.setUpdatedAt(now());
        profiles.updateById(owner);
        String usage = avatar ? MediaUsageCodes.PROFILE_AVATAR : MediaUsageCodes.PROFILE_RESUME;
        if (mediaAssetId != null) mediaReferences.attach(reference(owner.getId(), mediaAssetId, usage));
        if (oldId != null) mediaReferences.detach(reference(owner.getId(), oldId, usage));
        return changed(owner);
    }

    @Override
    @Transactional
    public ProfileVO addFeatured(FeaturedContentDTO request) {
        ProfileEntity owner = owner();
        if (request == null || request.getContentId() == null || request.getContentId() <= 0) {
            throw error("PROFILE_FEATURED_TARGET_NOT_FOUND", "精选目标不存在", 404);
        }
        String type = request.getContentType();
        if (type == null || !Set.of("TUTORIAL", "BLOG", "PORTFOLIO").contains(type)) {
            throw error("PROFILE_FEATURED_TYPE_INVALID", "精选内容类型无效", 400);
        }
        if (!targetExists(type, request.getContentId())) {
            throw error("PROFILE_FEATURED_TARGET_NOT_FOUND", "精选目标不存在", 404);
        }
        if (resolved(type, request.getContentId(), null, null) == null) {
            throw error("PROFILE_FEATURED_TARGET_NOT_PUBLISHED", "只能精选已公开内容", 409);
        }
        FeaturedContentEntity row = new FeaturedContentEntity();
        row.setProfileId(owner.getId());
        row.setContentType(type);
        row.setContentId(request.getContentId());
        row.setTitleOverride(optional(request.getTitleOverride(), 255, "精选标题"));
        row.setSortOrder(featured.selectCount(Wrappers.<FeaturedContentEntity>lambdaQuery()
                .eq(FeaturedContentEntity::getProfileId, owner.getId())).intValue());
        row.setStatus("ENABLED");
        row.setCreatedAt(now());
        row.setUpdatedAt(now());
        try {
            featured.insert(row);
        } catch (DuplicateKeyException exception) {
            throw error("PROFILE_FEATURED_TARGET_NOT_PUBLISHED", "该内容已经精选", 409);
        }
        return changed(owner);
    }

    @Override
    @Transactional
    public ProfileVO removeFeatured(Long id) {
        ProfileEntity owner = owner();
        featured.deleteById(feature(id, owner.getId()).getId());
        return changed(owner);
    }

    @Override
    @Transactional
    public ProfileVO orderFeatured(List<Long> ids) {
        ProfileEntity owner = owner();
        List<FeaturedContentEntity> rows = featured.selectList(Wrappers.<FeaturedContentEntity>lambdaQuery()
                .eq(FeaturedContentEntity::getProfileId, owner.getId()));
        checkOrder(ids, rows.stream().map(FeaturedContentEntity::getId).toList());
        for (FeaturedContentEntity row : rows) {
            row.setSortOrder(ids.indexOf(row.getId()));
            featured.updateById(row);
        }
        return changed(owner);
    }

    private ProfileVO changed(ProfileEntity owner) {
        events.afterCommit(owner.getId());
        return view(owner, true);
    }

    private ProfileVO view(ProfileEntity profile, boolean admin) {
        Long profileId = profile.getId();
        List<ProfileVO.Experience> experienceViews = experiences.selectList(Wrappers.<ExperienceEntity>lambdaQuery()
                        .eq(ExperienceEntity::getProfileId, profileId).eq(ExperienceEntity::getStatus, "ENABLED")
                        .orderByAsc(ExperienceEntity::getSortOrder, ExperienceEntity::getId))
                .stream().map(row -> new ProfileVO.Experience(String.valueOf(row.getId()), row.getExperienceType(),
                        row.getTitle(), row.getOrganization(), row.getStartDate(), row.getEndDate(),
                        row.getIsCurrent(), row.getDescriptionMd(), row.getSortOrder())).toList();
        List<ProfileVO.Skill> skillViews = skills.selectList(Wrappers.<SkillEntity>lambdaQuery()
                        .eq(SkillEntity::getProfileId, profileId).eq(SkillEntity::getStatus, "ENABLED")
                        .orderByAsc(SkillEntity::getCategory, SkillEntity::getSortOrder, SkillEntity::getId))
                .stream().map(row -> new ProfileVO.Skill(String.valueOf(row.getId()), row.getCategory(),
                        row.getName(), row.getDescription(), row.getProficiency(), row.getSortOrder())).toList();
        List<ProfileVO.SocialLink> socialViews = socials.selectList(Wrappers.<SocialLinkEntity>lambdaQuery()
                        .eq(SocialLinkEntity::getProfileId, profileId).eq(SocialLinkEntity::getStatus, "ENABLED")
                        .orderByAsc(SocialLinkEntity::getSortOrder, SocialLinkEntity::getId))
                .stream().map(row -> new ProfileVO.SocialLink(String.valueOf(row.getId()), row.getPlatformCode(),
                        row.getLabel(), row.getUrl(), row.getSortOrder())).toList();
        List<ProfileVO.Featured> featuredViews = featured.selectList(Wrappers.<FeaturedContentEntity>lambdaQuery()
                        .eq(FeaturedContentEntity::getProfileId, profileId)
                        .eq(FeaturedContentEntity::getStatus, "ENABLED")
                        .orderByAsc(FeaturedContentEntity::getSortOrder, FeaturedContentEntity::getId))
                .stream().map(row -> {
                    ProfileVO.Featured target = resolved(row.getContentType(), row.getContentId(),
                            row.getId(), row.getSortOrder());
                    if (target == null && admin) {
                        return new ProfileVO.Featured(String.valueOf(row.getId()), row.getContentType(),
                                String.valueOf(row.getContentId()),
                                row.getTitleOverride() == null ? "引用对象不可公开" : row.getTitleOverride(),
                                null, null, null, false, row.getSortOrder());
                    }
                    if (target != null && row.getTitleOverride() != null && !row.getTitleOverride().isBlank()) {
                        target.setTitle(row.getTitleOverride());
                    }
                    return target;
                }).filter(item -> item != null).toList();
        return ProfileVO.builder()
                .displayName(profile.getDisplayName()).headline(profile.getHeadline())
                .bioMarkdown(profile.getBioMarkdown()).locationText(profile.getLocationText())
                .avatarUrl(publicMediaUrl(profile.getAvatarMediaAssetId()))
                .resumeUrl(publicMediaUrl(profile.getResumeMediaAssetId()))
                .avatarMediaAssetId(admin && profile.getAvatarMediaAssetId() != null
                        ? String.valueOf(profile.getAvatarMediaAssetId()) : null)
                .resumeMediaAssetId(admin && profile.getResumeMediaAssetId() != null
                        ? String.valueOf(profile.getResumeMediaAssetId()) : null)
                .experiences(experienceViews).skills(skillViews).socialLinks(socialViews)
                .featuredContents(featuredViews).build();
    }

    private ProfileVO.Featured resolved(String type, Long id, Long referenceId, Integer order) {
        String rowId = referenceId == null ? null : String.valueOf(referenceId);
        String targetId = id == null ? null : String.valueOf(id);
        if ("TUTORIAL".equals(type)) {
            Optional<PublishedTutorial> result = tutorials.publishedTutorial(id);
            return result.map(item -> new ProfileVO.Featured(rowId, type, targetId, item.getTitle(),
                    item.getSummary(), "/tutorials/" + item.getSlug(), item.getCoverUrl(), true, order)).orElse(null);
        }
        if ("BLOG".equals(type)) {
            BlogPostSummary item = blogs.summary(id);
            return item == null ? null : new ProfileVO.Featured(rowId, type, targetId, item.getTitle(),
                    item.getSummary(), "/blog/posts/" + item.getSlug(), item.getCoverUrl(), true, order);
        }
        if ("PORTFOLIO".equals(type)) {
            Optional<PortfolioPublishedWork> result = works.publishedWork(id);
            return result.map(item -> new ProfileVO.Featured(rowId, type, targetId, item.getTitle(),
                    item.getSummary(), "/portfolio/" + item.getSlug(), item.getCoverUrl(), true, order)).orElse(null);
        }
        return null;
    }

    private boolean targetExists(String type, Long id) {
        return switch (type) {
            case "TUTORIAL" -> tutorials.exists(id);
            case "BLOG" -> blogs.exists(id);
            case "PORTFOLIO" -> works.exists(id);
            default -> false;
        };
    }

    private ProfileEntity owner() {
        ProfileEntity row = profiles.selectOne(Wrappers.<ProfileEntity>lambdaQuery()
                .eq(ProfileEntity::getProfileKey, "OWNER"));
        if (row == null) throw error("PROFILE_NOT_FOUND", "作者资料不存在", 404);
        return row;
    }

    private ExperienceEntity experience(Long id, Long profileId) {
        ExperienceEntity row = id == null ? null : experiences.selectById(id);
        if (row == null || !profileId.equals(row.getProfileId())) {
            throw error("PROFILE_EXPERIENCE_NOT_FOUND", "经历不存在", 404);
        }
        return row;
    }

    private SkillEntity skill(Long id, Long profileId) {
        SkillEntity row = id == null ? null : skills.selectById(id);
        if (row == null || !profileId.equals(row.getProfileId())) throw error("PROFILE_SKILL_NOT_FOUND", "条目不存在", 404);
        return row;
    }

    private SocialLinkEntity social(Long id, Long profileId) {
        SocialLinkEntity row = id == null ? null : socials.selectById(id);
        if (row == null || !profileId.equals(row.getProfileId())) throw error("PROFILE_SOCIAL_NOT_FOUND", "链接不存在", 404);
        return row;
    }

    private FeaturedContentEntity feature(Long id, Long profileId) {
        FeaturedContentEntity row = id == null ? null : featured.selectById(id);
        if (row == null || !profileId.equals(row.getProfileId())) throw error("PROFILE_FEATURED_NOT_FOUND", "精选内容不存在", 404);
        return row;
    }

    private String publicMediaUrl(Long assetId) {
        if (assetId == null) return null;
        MediaAssetSummary asset = media.get(assetId);
        return asset != null && "ACTIVE".equals(asset.getStatus()) && "PUBLIC".equals(asset.getAccessLevel())
                ? asset.getContentUrl() : null;
    }

    private MediaReferenceCommand reference(Long profileId, Long assetId, String usage) {
        return MediaReferenceCommand.builder().mediaAssetId(assetId).sourceModule("PROFILE")
                .sourceType("PROFILE").sourceId(profileId).usageCode(usage).build();
    }

    private String safeUrl(String value) {
        String url = required(value, 1000, "PROFILE_SOCIAL_URL_INVALID", "链接地址");
        try {
            URI uri = URI.create(url);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null) {
                throw error("PROFILE_SOCIAL_URL_INVALID", "只允许 http 或 https 链接", 400);
            }
        } catch (IllegalArgumentException exception) {
            throw error("PROFILE_SOCIAL_URL_INVALID", "链接地址无效", 400);
        }
        return url;
    }

    private void checkOrder(List<Long> ids, List<Long> existing) {
        if (ids == null || ids.size() != existing.size()
                || new HashSet<>(ids).size() != ids.size() || !new HashSet<>(ids).equals(new HashSet<>(existing))) {
            throw error("PROFILE_ORDER_INVALID", "顺序必须包含全部现有条目且不能重复", 400);
        }
    }

    private String required(String value, int max, String code, String field) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty() || normalized.length() > max) throw error(code, field + "长度无效", 400);
        return normalized;
    }

    private String optional(String value, int max, String field) {
        if (value == null) return null;
        String normalized = value.trim();
        if (normalized.length() > max) throw error("PROFILE_INVALID", field + "过长", 400);
        return normalized.isEmpty() ? null : normalized;
    }

    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    }

    private ApiException error(String code, String message, int status) {
        return new ApiException(code, message, status);
    }
}
