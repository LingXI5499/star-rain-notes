/*
 * ProfileServiceImpl：作者资料模块的业务实现文件。
 * 业务范围：个人介绍、社交链接、经历技能和精选内容。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 * 主要协作类型：BlogReferenceApi、BlogPostSummary、ApiException、MediaAssetApi、MediaReferenceApi、MediaAssetSummary、MediaReferenceCommand。
 * 阅读入口：publicProfile（公开资料处理入口）；summary（摘要处理入口）；publicProfileId（公开资料标识处理入口）；publishedDocument（已发布文档处理入口）。
 */

package com.starrainnotes.profile.service.impl;

import com.starrainnotes.blog.api.BlogReferenceApi;
import com.starrainnotes.blog.api.dto.BlogPostSummary;
import com.starrainnotes.common.exception.ApiException;
import com.starrainnotes.media.api.MediaAssetApi;
import com.starrainnotes.media.api.MediaReferenceApi;
import com.starrainnotes.media.api.dto.MediaAssetSummary;
import com.starrainnotes.media.api.dto.MediaReferenceCommand;
import com.starrainnotes.media.api.constant.MediaUsageCodes;
import com.starrainnotes.portfolio.api.PortfolioReferenceApi;
import com.starrainnotes.portfolio.api.dto.PortfolioPublishedWork;
import com.starrainnotes.profile.api.ProfilePublicApi;
import com.starrainnotes.profile.api.dto.ProfilePublishedDocument;
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
import com.starrainnotes.profile.api.vo.ProfileVO;
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

/**
 * 资料业务服务实现，属于作者资料模块。
 * 组织本模块的业务步骤，协调数据访问与其它接口。事务、权限和异常分支由具体方法说明。
 */
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService, ProfilePublicApi {
    /**
     * TYPES。
     */
    private static final Set<String> EXPERIENCE_TYPES = Set.of("EDUCATION", "PROJECT", "CAREER", "GROWTH", "OTHER");
    /**
     * CATEGORIES。
     */
    private static final Set<String> SKILL_CATEGORIES = Set.of("SKILL", "DIRECTION", "INTEREST");
    /**
     * PLATFORMS。
     */
    private static final Set<String> PLATFORMS = Set.of("GITHUB", "BILIBILI", "GITEE", "LINKEDIN", "EMAIL_PAGE", "OTHER");
    /**
     * profiles。
     * 协作类型：<code>ProfileMapper</code>。
     */
    private final ProfileMapper profiles;
    /**
     * experiences。
     * 协作类型：<code>ExperienceMapper</code>。
     */
    private final ExperienceMapper experiences;
    /**
     * skills。
     * 协作类型：<code>SkillMapper</code>。
     */
    private final SkillMapper skills;
    /**
     * socials。
     * 协作类型：<code>SocialLinkMapper</code>。
     */
    private final SocialLinkMapper socials;
    /**
     * 精选。
     * 协作类型：<code>FeaturedContentMapper</code>。
     */
    private final FeaturedContentMapper featured;
    /**
     * 媒体。
     * 协作类型：<code>MediaAssetApi</code>。
     */
    private final MediaAssetApi media;
    /**
     * 媒体References。
     * 协作类型：<code>MediaReferenceApi</code>。
     */
    private final MediaReferenceApi mediaReferences;
    /**
     * tutorials。
     * 协作类型：<code>TutorialReferenceApi</code>。
     */
    private final TutorialReferenceApi tutorials;
    /**
     * blogs。
     * 协作类型：<code>BlogReferenceApi</code>。
     */
    private final BlogReferenceApi blogs;
    /**
     * 作品列表。
     * 协作类型：<code>PortfolioReferenceApi</code>。
     */
    private final PortfolioReferenceApi works;
    /**
     * 事件集合。
     */
    private final ProfileEventPublisher events;

    /**
     * 公开资料处理入口。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 关键处理步骤调用 <code>owner</code>、<code>profile.getStatus</code>、<code>error</code>、<code>view</code>。
     * @return 类型为 <code>ProfileVO</code> 的处理结果
     */
    @Override
    @Transactional(readOnly = true)
    public ProfileVO publicProfile() {
        ProfileEntity profile = owner();
        if (!"PUBLIC".equals(profile.getStatus())) {
            throw error("PROFILE_HIDDEN", "作者资料暂未公开", 404);
        }
        return view(profile, false);
    }

    /**
     * 摘要处理入口。
     * 关键处理步骤调用 <code>publicProfile</code>。
     * @return 类型为 <code>ProfileVO</code> 的处理结果
     */
    @Override
    public ProfileVO summary() {
        return publicProfile();
    }

    /**
     * 公开资料标识处理入口。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 关键处理步骤调用 <code>owner</code>、<code>profile.getStatus</code>、<code>profile.getId</code>。
     * @return 类型为 <code>Long</code> 的处理结果
     */
    @Override
    @Transactional(readOnly = true)
    public Long publicProfileId() {
        ProfileEntity profile = owner();
        return "PUBLIC".equals(profile.getStatus()) ? profile.getId() : null;
    }

    /**
     * 已发布文档处理入口。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 关键处理步骤调用 <code>publicProfileId</code>、<code>summary</code>、<code>profile.getBioMarkdown</code>、<code>profile.getExperiences</code>。
     * @return 类型为 <code>ProfilePublishedDocument</code> 的处理结果
     */
    @Override
    @Transactional(readOnly = true)
    public ProfilePublishedDocument publishedDocument() {
        Long id = publicProfileId();
        if (id == null) return null;
        ProfileVO profile = summary();
        StringBuilder body = new StringBuilder(profile.getBioMarkdown() == null ? "" : profile.getBioMarkdown());
        if (profile.getExperiences() != null) profile.getExperiences().forEach(item -> {
            body.append(' ').append(item.getTitle());
            body.append(' ').append(item.getOrganization());
            body.append(' ').append(item.getDescriptionMd());
        });
        if (profile.getSkills() != null) profile.getSkills().forEach(item -> body.append(' ').append(item.getName()));
        return new ProfilePublishedDocument(id, profile.getDisplayName(), profile.getHeadline(), body.toString());
    }

    /**
     * 后台资料处理入口。
     * 事务声明：<code>@Transactional(readOnly = true)</code>，该方法的数据库操作按此事务配置执行。
     * 关键处理步骤调用 <code>view</code>、<code>owner</code>。
     * @return 类型为 <code>ProfileVO</code> 的处理结果
     */
    @Override
    @Transactional(readOnly = true)
    public ProfileVO adminProfile() {
        return view(owner(), true);
    }

    /**
     * 更新Basic。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>profiles.updateBasic</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>ProfilePatchDTO</code>
     * @return 类型为 <code>ProfileVO</code> 的处理结果
     */
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
        profiles.updateBasic(row);
        return changed(row);
    }

    /**
     * 保存Experience。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>EXPERIENCE_TYPES.contains</code>、<code>experiences.countByProfileId</code>、<code>experiences.insert</code>、<code>experiences.updateContent</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>ExperienceDTO</code>
     * @return 类型为 <code>ProfileVO</code> 的处理结果
     */
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
            row.setSortOrder((int) experiences.countByProfileId(owner.getId()));
            row.setStatus("ENABLED");
            row.setCreatedAt(now());
            row.setUpdatedAt(now());
            experiences.insert(row);
        } else {
            row.setUpdatedAt(now());
            experiences.updateContent(row);
        }
        return changed(owner);
    }

    /**
     * 移除Experience。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>experiences.deleteById</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>ProfileVO</code> 的处理结果
     */
    @Override
    @Transactional
    public ProfileVO removeExperience(Long id) {
        ProfileEntity owner = owner();
        experiences.deleteById(experience(id, owner.getId()).getId());
        return changed(owner);
    }

    /**
     * 顺序Experiences处理入口。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>experiences.listByProfileId</code>、<code>experiences.updateSortOrder</code>。
     *
     * @param ids 标识集合；类型为 <code>List&lt;Long&gt;</code>
     * @return 类型为 <code>ProfileVO</code> 的处理结果
     */
    @Override
    @Transactional
    public ProfileVO orderExperiences(List<Long> ids) {
        ProfileEntity owner = owner();
        List<ExperienceEntity> rows = experiences.listByProfileId(owner.getId());
        checkOrder(ids, rows.stream().map(ExperienceEntity::getId).toList());
        for (ExperienceEntity row : rows) {
            row.setSortOrder(ids.indexOf(row.getId()));
            experiences.updateSortOrder(row);
        }
        return changed(owner);
    }

    /**
     * 保存Skill。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>SKILL_CATEGORIES.contains</code>、<code>skills.countByProfileId</code>、<code>skills.insert</code>、<code>skills.updateContent</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>SkillDTO</code>
     * @return 类型为 <code>ProfileVO</code> 的处理结果
     */
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
                row.setSortOrder((int) skills.countByProfileId(owner.getId()));
                row.setStatus("ENABLED");
                row.setCreatedAt(now());
                row.setUpdatedAt(now());
                skills.insert(row);
            } else {
                row.setUpdatedAt(now());
                skills.updateContent(row);
            }
        } catch (DuplicateKeyException exception) {
            throw error("PROFILE_SKILL_CONFLICT", "同类别下已有同名条目", 409);
        }
        return changed(owner);
    }

    /**
     * 移除Skill。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>skills.deleteById</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>ProfileVO</code> 的处理结果
     */
    @Override
    @Transactional
    public ProfileVO removeSkill(Long id) {
        ProfileEntity owner = owner();
        skills.deleteById(skill(id, owner.getId()).getId());
        return changed(owner);
    }

    /**
     * 顺序Skills处理入口。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>skills.listByProfileId</code>、<code>skills.updateSortOrder</code>。
     *
     * @param ids 标识集合；类型为 <code>List&lt;Long&gt;</code>
     * @return 类型为 <code>ProfileVO</code> 的处理结果
     */
    @Override
    @Transactional
    public ProfileVO orderSkills(List<Long> ids) {
        ProfileEntity owner = owner();
        List<SkillEntity> rows = skills.listByProfileId(owner.getId());
        checkOrder(ids, rows.stream().map(SkillEntity::getId).toList());
        for (SkillEntity row : rows) {
            row.setSortOrder(ids.indexOf(row.getId()));
            skills.updateSortOrder(row);
        }
        return changed(owner);
    }

    /**
     * 保存Social。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>PLATFORMS.contains</code>、<code>socials.countByProfileId</code>、<code>socials.insert</code>、<code>socials.updateContent</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>SocialLinkDTO</code>
     * @return 类型为 <code>ProfileVO</code> 的处理结果
     */
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
                row.setSortOrder((int) socials.countByProfileId(owner.getId()));
                row.setStatus("ENABLED");
                row.setCreatedAt(now());
                row.setUpdatedAt(now());
                socials.insert(row);
            } else {
                row.setUpdatedAt(now());
                socials.updateContent(row);
            }
        } catch (DuplicateKeyException exception) {
            throw error("PROFILE_SOCIAL_URL_INVALID", "同一平台只能添加一条链接", 409);
        }
        return changed(owner);
    }

    /**
     * 移除Social。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>socials.deleteById</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>ProfileVO</code> 的处理结果
     */
    @Override
    @Transactional
    public ProfileVO removeSocial(Long id) {
        ProfileEntity owner = owner();
        socials.deleteById(social(id, owner.getId()).getId());
        return changed(owner);
    }

    /**
     * 顺序Social处理入口。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>socials.listByProfileId</code>、<code>socials.updateSortOrder</code>。
     *
     * @param ids 标识集合；类型为 <code>List&lt;Long&gt;</code>
     * @return 类型为 <code>ProfileVO</code> 的处理结果
     */
    @Override
    @Transactional
    public ProfileVO orderSocial(List<Long> ids) {
        ProfileEntity owner = owner();
        List<SocialLinkEntity> rows = socials.listByProfileId(owner.getId());
        checkOrder(ids, rows.stream().map(SocialLinkEntity::getId).toList());
        for (SocialLinkEntity row : rows) {
            row.setSortOrder(ids.indexOf(row.getId()));
            socials.updateSortOrder(row);
        }
        return changed(owner);
    }

    /**
     * 设置媒体。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>media.assertUsable</code>、<code>media.get</code>、<code>profiles.updateMediaAssets</code>、<code>mediaReferences.attach</code>、<code>mediaReferences.detach</code>。
     *
     * @param kind kind；类型为 <code>String</code>
     * @param mediaAssetId 关联的媒体资产标识；类型为 <code>Long</code>
     * @return 类型为 <code>ProfileVO</code> 的处理结果
     */
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
        profiles.updateMediaAssets(owner);
        String usage = avatar ? MediaUsageCodes.PROFILE_AVATAR : MediaUsageCodes.PROFILE_RESUME;
        if (mediaAssetId != null) mediaReferences.attach(reference(owner.getId(), mediaAssetId, usage));
        if (oldId != null) mediaReferences.detach(reference(owner.getId(), oldId, usage));
        return changed(owner);
    }

    /**
     * 添加精选。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>featured.countByProfileId</code>、<code>featured.insert</code>。
     *
     * @param request 本次操作的请求参数对象；类型为 <code>FeaturedContentDTO</code>
     * @return 类型为 <code>ProfileVO</code> 的处理结果
     */
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
        row.setSortOrder((int) featured.countByProfileId(owner.getId()));
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

    /**
     * 移除精选。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>featured.deleteById</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 类型为 <code>ProfileVO</code> 的处理结果
     */
    @Override
    @Transactional
    public ProfileVO removeFeatured(Long id) {
        ProfileEntity owner = owner();
        featured.deleteById(feature(id, owner.getId()).getId());
        return changed(owner);
    }

    /**
     * 顺序精选处理入口。
     * 事务声明：<code>@Transactional</code>，该方法的数据库操作按此事务配置执行。
     * 主要协作调用：<code>featured.listByProfileId</code>、<code>featured.updateSortOrder</code>。
     *
     * @param ids 标识集合；类型为 <code>List&lt;Long&gt;</code>
     * @return 类型为 <code>ProfileVO</code> 的处理结果
     */
    @Override
    @Transactional
    public ProfileVO orderFeatured(List<Long> ids) {
        ProfileEntity owner = owner();
        List<FeaturedContentEntity> rows = featured.listByProfileId(owner.getId());
        checkOrder(ids, rows.stream().map(FeaturedContentEntity::getId).toList());
        for (FeaturedContentEntity row : rows) {
            row.setSortOrder(ids.indexOf(row.getId()));
            featured.updateSortOrder(row);
        }
        return changed(owner);
    }

    /**
     * changed处理入口。
     * 主要协作调用：<code>events.afterCommit</code>。
     *
     * @param owner owner；类型为 <code>ProfileEntity</code>
     * @return 类型为 <code>ProfileVO</code> 的处理结果
     */
    private ProfileVO changed(ProfileEntity owner) {
        events.afterCommit(owner.getId());
        return view(owner, true);
    }

    /**
     * view处理入口。
     * 主要协作调用：<code>experiences.listEnabledByProfileId</code>、<code>skills.listEnabledByProfileId</code>、<code>socials.listEnabledByProfileId</code>、<code>featured.listEnabledByProfileId</code>。
     *
     * @param profile 资料；类型为 <code>ProfileEntity</code>
     * @param admin 后台；类型为 <code>boolean</code>
     * @return 类型为 <code>ProfileVO</code> 的处理结果
     */
    private ProfileVO view(ProfileEntity profile, boolean admin) {
        Long profileId = profile.getId();
        List<ProfileVO.Experience> experienceViews = experiences.listEnabledByProfileId(profileId)
                .stream().map(row -> new ProfileVO.Experience(String.valueOf(row.getId()), row.getExperienceType(),
                        row.getTitle(), row.getOrganization(), row.getStartDate(), row.getEndDate(),
                        row.getIsCurrent(), row.getDescriptionMd(), row.getSortOrder())).toList();
        List<ProfileVO.Skill> skillViews = skills.listEnabledByProfileId(profileId)
                .stream().map(row -> new ProfileVO.Skill(String.valueOf(row.getId()), row.getCategory(),
                        row.getName(), row.getDescription(), row.getProficiency(), row.getSortOrder())).toList();
        List<ProfileVO.SocialLink> socialViews = socials.listEnabledByProfileId(profileId)
                .stream().map(row -> new ProfileVO.SocialLink(String.valueOf(row.getId()), row.getPlatformCode(),
                        row.getLabel(), row.getUrl(), row.getSortOrder())).toList();
        List<ProfileVO.Featured> featuredViews = featured.listEnabledByProfileId(profileId)
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

    /**
     * resolved处理入口。
     * 主要协作调用：<code>tutorials.publishedTutorial</code>、<code>blogs.summary</code>、<code>works.publishedWork</code>。
     *
     * @param type 类型；类型为 <code>String</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param referenceId 引用标识；类型为 <code>Long</code>
     * @param order 顺序；类型为 <code>Integer</code>
     * @return 类型为 <code>ProfileVO.Featured</code> 的处理结果
     */
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

    /**
     * 目标存在性处理入口。
     * 主要协作调用：<code>tutorials.exists</code>、<code>blogs.exists</code>、<code>works.exists</code>。
     *
     * @param type 类型；类型为 <code>String</code>
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @return 本方法条件是否成立的判断结果
     */
    private boolean targetExists(String type, Long id) {
        return switch (type) {
            case "TUTORIAL" -> tutorials.exists(id);
            case "BLOG" -> blogs.exists(id);
            case "PORTFOLIO" -> works.exists(id);
            default -> false;
        };
    }

    /**
     * owner处理入口。
     * 主要协作调用：<code>profiles.selectOwner</code>。
     * @return 类型为 <code>ProfileEntity</code> 的处理结果
     */
    private ProfileEntity owner() {
        ProfileEntity row = profiles.selectOwner();
        if (row == null) throw error("PROFILE_NOT_FOUND", "作者资料不存在", 404);
        return row;
    }

    /**
     * experience处理入口。
     * 主要协作调用：<code>experiences.selectById</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param profileId 资料标识；类型为 <code>Long</code>
     * @return 类型为 <code>ExperienceEntity</code> 的处理结果
     */
    private ExperienceEntity experience(Long id, Long profileId) {
        ExperienceEntity row = id == null ? null : experiences.selectById(id);
        if (row == null || !profileId.equals(row.getProfileId())) {
            throw error("PROFILE_EXPERIENCE_NOT_FOUND", "经历不存在", 404);
        }
        return row;
    }

    /**
     * skill处理入口。
     * 主要协作调用：<code>skills.selectById</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param profileId 资料标识；类型为 <code>Long</code>
     * @return 类型为 <code>SkillEntity</code> 的处理结果
     */
    private SkillEntity skill(Long id, Long profileId) {
        SkillEntity row = id == null ? null : skills.selectById(id);
        if (row == null || !profileId.equals(row.getProfileId())) throw error("PROFILE_SKILL_NOT_FOUND", "条目不存在", 404);
        return row;
    }

    /**
     * social处理入口。
     * 主要协作调用：<code>socials.selectById</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param profileId 资料标识；类型为 <code>Long</code>
     * @return 类型为 <code>SocialLinkEntity</code> 的处理结果
     */
    private SocialLinkEntity social(Long id, Long profileId) {
        SocialLinkEntity row = id == null ? null : socials.selectById(id);
        if (row == null || !profileId.equals(row.getProfileId())) throw error("PROFILE_SOCIAL_NOT_FOUND", "链接不存在", 404);
        return row;
    }

    /**
     * 功能处理入口。
     * 主要协作调用：<code>featured.selectById</code>。
     *
     * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>
     * @param profileId 资料标识；类型为 <code>Long</code>
     * @return 类型为 <code>FeaturedContentEntity</code> 的处理结果
     */
    private FeaturedContentEntity feature(Long id, Long profileId) {
        FeaturedContentEntity row = id == null ? null : featured.selectById(id);
        if (row == null || !profileId.equals(row.getProfileId())) throw error("PROFILE_FEATURED_NOT_FOUND", "精选内容不存在", 404);
        return row;
    }

    /**
     * 公开媒体地址处理入口。
     * 主要协作调用：<code>media.get</code>。
     *
     * @param assetId 资产标识；类型为 <code>Long</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private String publicMediaUrl(Long assetId) {
        if (assetId == null) return null;
        MediaAssetSummary asset = media.get(assetId);
        return asset != null && "ACTIVE".equals(asset.getStatus()) && "PUBLIC".equals(asset.getAccessLevel())
                ? asset.getContentUrl() : null;
    }

    /**
     * 引用处理入口。
     * 关键处理步骤调用 <code>MediaReferenceCommand.builder</code>。
     *
     * @param profileId 资料标识；类型为 <code>Long</code>
     * @param assetId 资产标识；类型为 <code>Long</code>
     * @param usage usage；类型为 <code>String</code>
     * @return 类型为 <code>MediaReferenceCommand</code> 的处理结果
     */
    private MediaReferenceCommand reference(Long profileId, Long assetId, String usage) {
        return MediaReferenceCommand.builder().mediaAssetId(assetId).sourceModule("PROFILE")
                .sourceType("PROFILE").sourceId(profileId).usageCode(usage).build();
    }

    /**
     * 安全地址处理入口。
     * 关键处理步骤调用 <code>required</code>、<code>URI.create</code>、<code>uri.getScheme</code>、<code>uri.getHost</code>。
     *
     * @param value 值；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
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

    /**
     * 检查顺序。
     * 关键处理步骤调用 <code>ids.size</code>、<code>existing.size</code>、<code>error</code>。
     *
     * @param ids 标识集合；类型为 <code>List&lt;Long&gt;</code>
     * @param existing existing；类型为 <code>List&lt;Long&gt;</code>
     */
    private void checkOrder(List<Long> ids, List<Long> existing) {
        if (ids == null || ids.size() != existing.size()
                || new HashSet<>(ids).size() != ids.size() || !new HashSet<>(ids).equals(new HashSet<>(existing))) {
            throw error("PROFILE_ORDER_INVALID", "顺序必须包含全部现有条目且不能重复", 400);
        }
    }

    /**
     * 必需处理入口。
     * 关键处理步骤调用 <code>value.trim</code>、<code>normalized.isEmpty</code>、<code>normalized.length</code>、<code>error</code>。
     *
     * @param value 值；类型为 <code>String</code>
     * @param max 最大；类型为 <code>int</code>
     * @param code 代码；类型为 <code>String</code>
     * @param field 字段；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private String required(String value, int max, String code, String field) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty() || normalized.length() > max) throw error(code, field + "长度无效", 400);
        return normalized;
    }

    /**
     * optional处理入口。
     * 关键处理步骤调用 <code>value.trim</code>、<code>normalized.length</code>、<code>error</code>、<code>normalized.isEmpty</code>。
     *
     * @param value 值；类型为 <code>String</code>
     * @param max 最大；类型为 <code>int</code>
     * @param field 字段；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private String optional(String value, int max, String field) {
        if (value == null) return null;
        String normalized = value.trim();
        if (normalized.length() > max) throw error("PROFILE_INVALID", field + "过长", 400);
        return normalized.isEmpty() ? null : normalized;
    }

    /**
     * now处理入口。
     * 关键处理步骤调用 <code>LocalDateTime.now</code>。
     * @return 类型为 <code>LocalDateTime</code> 的处理结果
     */
    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    }

    /**
     * 错误处理入口。
     *
     * @param code 代码；类型为 <code>String</code>
     * @param message 留言；类型为 <code>String</code>
     * @param status 当前业务状态，合法取值由本模块状态定义和状态转换规则约束；类型为 <code>int</code>
     * @return 类型为 <code>ApiException</code> 的处理结果
     */
    private ApiException error(String code, String message, int status) {
        return new ApiException(code, message, status);
    }
}
