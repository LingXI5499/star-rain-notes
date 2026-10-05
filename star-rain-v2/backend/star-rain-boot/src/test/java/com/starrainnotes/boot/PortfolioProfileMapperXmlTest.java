package com.starrainnotes.boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.starrainnotes.english.overview.entity.EnglishOverviewEntity;
import com.starrainnotes.english.overview.mapper.EnglishOverviewMapper;
import com.starrainnotes.portfolio.entity.WorkDetailEntity;
import com.starrainnotes.portfolio.entity.WorkEntity;
import com.starrainnotes.portfolio.entity.WorkLinkEntity;
import com.starrainnotes.portfolio.entity.WorkMediaEntity;
import com.starrainnotes.portfolio.mapper.WorkDetailMapper;
import com.starrainnotes.portfolio.mapper.WorkLinkMapper;
import com.starrainnotes.portfolio.mapper.WorkMapper;
import com.starrainnotes.portfolio.mapper.WorkMediaMapper;
import com.starrainnotes.profile.entity.ExperienceEntity;
import com.starrainnotes.profile.entity.FeaturedContentEntity;
import com.starrainnotes.profile.entity.ProfileEntity;
import com.starrainnotes.profile.entity.SkillEntity;
import com.starrainnotes.profile.entity.SocialLinkEntity;
import com.starrainnotes.profile.mapper.ExperienceMapper;
import com.starrainnotes.profile.mapper.FeaturedContentMapper;
import com.starrainnotes.profile.mapper.ProfileMapper;
import com.starrainnotes.profile.mapper.SkillMapper;
import com.starrainnotes.profile.mapper.SocialLinkMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/*
 * portfolio / profile / english 三个模块 10 个 Mapper 的真实数据库读写验证。
 *
 * 这些 Mapper 刚从 MyBatis-Plus 的 BaseMapper 迁到「接口显式方法 + XML」。Mockito 单元测试
 * 把 Mapper 整个 mock 掉，测不到迁移最容易出错的那一层：
 *   1. XML 的列清单、自增主键回填、JSON 列按字符串读回；
 *   2. 一次 updateById 拆成几条语义化 UPDATE 之后，每条到底改了哪几列 ——
 *      尤其「只改排序不动 caption / label / 正文」；
 *   3. 原先带 @TableField(updateStrategy = FieldStrategy.ALWAYS) 的列（profile 的文案与媒体引用、
 *      skill 的说明、experience 的组织与日期）必须「传 null 就写成 NULL」，不能被 NOT_NULL 策略吞掉；
 *      反过来 portfolio 的 caption 走的是 NOT_NULL 策略，「不传就保留旧值」也要保持。
 *
 * 全程在一个不 commit 的 SqlSession 里跑，@AfterEach 回滚；最后一个用例用新连接断言库里没有残留，
 * 并核对被改过的 OWNER 行（sr_profile 单行资料）已经回到测试前的值。
 */
class PortfolioProfileMapperXmlTest extends MapperXmlIntegrationSupport {

    private static final AtomicLong PROBE = new AtomicLong(System.nanoTime() % 1_000_000L);

    private SqlSession session;
    private WorkMapper works;
    private WorkDetailMapper details;
    private WorkMediaMapper media;
    private WorkLinkMapper links;
    private ProfileMapper profiles;
    private ExperienceMapper experiences;
    private SkillMapper skills;
    private SocialLinkMapper socials;
    private FeaturedContentMapper featured;
    private EnglishOverviewMapper englishOverview;

    @BeforeEach
    void open() {
        session = openSession();
        works = session.getMapper(WorkMapper.class);
        details = session.getMapper(WorkDetailMapper.class);
        media = session.getMapper(WorkMediaMapper.class);
        links = session.getMapper(WorkLinkMapper.class);
        profiles = session.getMapper(ProfileMapper.class);
        experiences = session.getMapper(ExperienceMapper.class);
        skills = session.getMapper(SkillMapper.class);
        socials = session.getMapper(SocialLinkMapper.class);
        featured = session.getMapper(FeaturedContentMapper.class);
        englishOverview = session.getMapper(EnglishOverviewMapper.class);
    }

    @AfterEach
    void rollback() {
        if (session != null) {
            session.rollback();
            session.close();
        }
    }

    @Test
    void everyMigratedMapperIsReachableThroughTheSession() {
        assertNotNull(works);
        assertNotNull(details);
        assertNotNull(media);
        assertNotNull(links);
        assertNotNull(profiles);
        assertNotNull(experiences);
        assertNotNull(skills);
        assertNotNull(socials);
        assertNotNull(featured);
        assertNotNull(englishOverview);
    }

    @Test
    void workMapperRoundTripsFiltersAndEveryUpdateKeepsUntouchedColumns() {
        /* keyword 过滤查的是 title，所以唯一标记放标题里；slug 另用一个唯一值 */
        String keyword = unique("mapperxmlwork");
        String slug = unique("mapperxmlslug");
        WorkEntity created = newWork(slug, keyword, "OTHER", "DRAFT");
        assertEquals(1, works.insert(created));
        assertNotNull(created.getId(), "insert 没有回填自增主键");

        WorkEntity loaded = works.selectById(created.getId());
        assertNotNull(loaded, "selectById 读不到刚插入的行");
        assertEquals(slug, loaded.getSlug());
        assertEquals(keyword, loaded.getTitle());
        assertEquals("# 正文", loaded.getBodyMarkdown());
        assertEquals("DRAFT", loaded.getStatus());
        assertNotNull(loaded.getCreatedAt(), "created_at 没有映射回来，检查列清单");
        assertNotNull(loaded.getUpdatedAt());
        assertNotNull(loaded.getCreatedByAccountId());
        assertNotNull(works.byIdForUpdate(created.getId()), "FOR UPDATE 读不到刚插入的行");

        // 公开读取按 slug + status 过滤，草稿不能出现在公开路径里
        assertNotNull(works.selectBySlugAndStatus(slug, "DRAFT"));
        assertNull(works.selectBySlugAndStatus(slug, "PUBLISHED"));
        assertNotNull(works.selectByIdAndStatus(created.getId(), "DRAFT"));
        assertNull(works.selectByIdAndStatus(created.getId(), "PUBLISHED"));

        // 三个筛选条件各自独立生效（<if> 分支：全 null / 只 status / 只 workType）
        assertEquals(1, works.countByFilter(null, null, keyword), "keyword 过滤没生效");
        assertEquals(1, works.countByFilter("OTHER", null, keyword));
        assertEquals(0, works.countByFilter("VIDEO", null, keyword), "work_type 过滤没生效");
        assertEquals(1, works.countByFilter(null, "DRAFT", keyword), "status 过滤没生效");
        assertEquals(0, works.countByFilter(null, "PUBLISHED", keyword));
        assertEquals(1, works.countBySlug(slug), "slug 查重没生效");

        List<WorkEntity> page = works.pageByFilter(null, "DRAFT", keyword, 10, 0);
        assertEquals(1, page.size(), "LIMIT/OFFSET 或筛选条件没生效");
        assertEquals(created.getId(), page.get(0).getId());
        assertTrue(works.pageByFilter(null, "DRAFT", keyword, 10, 1).isEmpty(), "OFFSET 没有生效");

        // 改标题：正文、状态、创建人必须保持原值（标题保留标记，后面的 keyword 过滤仍只命中这一行）
        LocalDateTime at = now();
        loaded.setTitle(keyword + "-renamed");
        loaded.setSlug(slug + "-renamed");
        loaded.setUpdatedByAccountId(probeId());
        loaded.setUpdatedAt(at);
        assertEquals(1, works.updateContent(loaded));
        WorkEntity renamed = works.selectById(created.getId());
        assertEquals(keyword + "-renamed", renamed.getTitle());
        assertEquals(slug + "-renamed", renamed.getSlug());
        assertEquals("# 正文", renamed.getBodyMarkdown(), "改基础信息不应动正文");
        assertEquals("DRAFT", renamed.getStatus(), "改基础信息不应动状态");
        assertEquals(loaded.getCreatedByAccountId(), renamed.getCreatedByAccountId());

        // 改正文：标题、摘要、地址必须保持原值
        renamed.setBodyMarkdown("# 新正文");
        renamed.setUpdatedByAccountId(probeId());
        renamed.setUpdatedAt(now());
        assertEquals(1, works.updateBody(renamed));
        WorkEntity withBody = works.selectById(created.getId());
        assertEquals("# 新正文", withBody.getBodyMarkdown());
        assertEquals(keyword + "-renamed", withBody.getTitle(), "改正文不应动标题");
        assertEquals(slug + "-renamed", withBody.getSlug(), "改正文不应动地址");
        assertEquals("摘要", withBody.getSummary(), "改正文不应动摘要");

        // touch：媒体/外链变更只更新审计列
        long toucher = probeId();
        withBody.setUpdatedByAccountId(toucher);
        withBody.setUpdatedAt(now());
        assertEquals(1, works.touch(withBody));
        WorkEntity touched = works.selectById(created.getId());
        assertEquals(toucher, touched.getUpdatedByAccountId().longValue());
        assertEquals("# 新正文", touched.getBodyMarkdown(), "touch 不应动正文");
        assertEquals("DRAFT", touched.getStatus(), "touch 不应动状态");

        // 发布：写入发布时间，正文与地址不受影响
        touched.setStatus("PUBLISHED");
        touched.setPublishedAt(now());
        touched.setUpdatedByAccountId(probeId());
        touched.setUpdatedAt(now());
        assertEquals(1, works.publish(touched));
        WorkEntity published = works.selectById(created.getId());
        assertEquals("PUBLISHED", published.getStatus());
        assertNotNull(published.getPublishedAt());
        assertEquals("# 新正文", published.getBodyMarkdown(), "发布不应动正文");
        assertNull(published.getWithdrawnAt());
        assertNotNull(works.selectBySlugAndStatus(slug + "-renamed", "PUBLISHED"));
        assertEquals(1, works.pageByFilter(null, "PUBLISHED", keyword, 10, 0).size());

        // 撤回：记撤回时间，首次发布时间保留
        published.setStatus("WITHDRAWN");
        published.setWithdrawnAt(now());
        published.setUpdatedByAccountId(probeId());
        published.setUpdatedAt(now());
        assertEquals(1, works.withdraw(published));
        WorkEntity withdrawn = works.selectById(created.getId());
        assertEquals("WITHDRAWN", withdrawn.getStatus());
        assertNotNull(withdrawn.getWithdrawnAt());
        assertEquals(published.getPublishedAt(), withdrawn.getPublishedAt(), "撤回不应改动首次发布时间");

        // 恢复：只改状态，published_at / withdrawn_at 都不参与 SET
        withdrawn.setStatus("PUBLISHED");
        withdrawn.setUpdatedByAccountId(probeId());
        withdrawn.setUpdatedAt(now());
        assertEquals(1, works.restore(withdrawn));
        WorkEntity restored = works.selectById(created.getId());
        assertEquals("PUBLISHED", restored.getStatus());
        assertEquals(published.getPublishedAt(), restored.getPublishedAt(),
                "恢复不应覆盖首次发布时间");
        assertNotNull(restored.getWithdrawnAt(), "恢复不应清掉撤回时间（原先 updateById 也不会）");

        assertEquals(1, works.deleteById(created.getId()));
        assertNull(works.selectById(created.getId()));
    }

    @Test
    void workDetailMapperRoundTripsJsonAndDelete() {
        WorkEntity work = insertWork();

        WorkDetailEntity detail = new WorkDetailEntity();
        detail.setWorkId(work.getId());
        detail.setWorkType("OTHER");
        detail.setDetailJson("{\"note\":\"首次\"}");
        assertEquals(1, details.insert(detail));
        assertNotNull(detail.getId(), "insert 没有回填自增主键");

        WorkDetailEntity loaded = details.selectByWorkId(work.getId());
        assertNotNull(loaded);
        assertEquals("OTHER", loaded.getWorkType());
        assertTrue(loaded.getDetailJson().contains("首次"), "JSON 列没有按字符串映射回来");
        assertNotNull(loaded.getCreatedAt());
        assertNull(details.selectByWorkId(probeId()), "不存在的 work_id 应当返回 null");

        loaded.setDetailJson("{\"note\":\"第二次\"}");
        assertEquals(1, details.updateDetailJson(loaded));
        WorkDetailEntity updated = details.selectByWorkId(work.getId());
        assertTrue(updated.getDetailJson().contains("第二次"));
        assertEquals(work.getId(), updated.getWorkId(), "更新详情不应改动 work_id");
        assertEquals("OTHER", updated.getWorkType(), "更新详情不应改动 work_type");

        assertEquals(1, details.deleteById(updated.getId()));
        assertNull(details.selectByWorkId(work.getId()));
    }

    @Test
    void workMediaMapperKeepsCaptionAndAssetOnSortOnlyUpdates() {
        WorkEntity work = insertWork();
        long assetId = probeId();

        WorkMediaEntity row = new WorkMediaEntity();
        row.setWorkId(work.getId());
        row.setMediaAssetId(assetId);
        row.setUsageType("SCREENSHOT");
        row.setCaption("初始说明");
        row.setSortOrder(5);
        assertEquals(1, media.insert(row));
        assertNotNull(row.getId(), "insert 没有回填自增主键");

        WorkMediaEntity loaded = media.selectById(row.getId());
        assertNotNull(loaded);
        assertEquals("初始说明", loaded.getCaption());
        assertEquals(5, loaded.getSortOrder().intValue());
        assertNotNull(loaded.getCreatedAt());
        assertEquals(1, media.listByWorkId(work.getId()).size());

        // 改内容：work_id 不参与 SET
        loaded.setUsageType("ATTACHMENT");
        loaded.setCaption("改后的说明");
        loaded.setSortOrder(9);
        assertEquals(1, media.updateContent(loaded));
        WorkMediaEntity contentUpdated = media.selectById(row.getId());
        assertEquals("ATTACHMENT", contentUpdated.getUsageType());
        assertEquals("改后的说明", contentUpdated.getCaption());
        assertEquals(9, contentUpdated.getSortOrder().intValue());
        assertEquals(assetId, contentUpdated.getMediaAssetId(), "更新内容不应改动媒体资源");
        assertEquals(work.getId(), contentUpdated.getWorkId(), "更新内容不应改动归属作品");

        // 只改排序：caption / usage_type / media_asset_id 必须保持原值
        contentUpdated.setSortOrder(77);
        assertEquals(1, media.updateSortOrder(contentUpdated));
        WorkMediaEntity sorted = media.selectById(row.getId());
        assertEquals(77, sorted.getSortOrder().intValue());
        assertEquals("改后的说明", sorted.getCaption(), "改排序不应动 caption");
        assertEquals("ATTACHMENT", sorted.getUsageType(), "改排序不应动 usage_type");
        assertEquals(assetId, sorted.getMediaAssetId(), "改排序不应动 media_asset_id");

        // caption 走原先 updateById 的 NOT_NULL 策略：不传（null）时保留旧值
        sorted.setCaption(null);
        assertEquals(1, media.updateContent(sorted));
        assertEquals("改后的说明", media.selectById(row.getId()).getCaption(),
                "caption 传 null 被清空了，NOT_NULL 更新语义在迁移中丢了");

        assertEquals(1, media.deleteById(row.getId()));
        assertNull(media.selectById(row.getId()));
        assertTrue(media.listByWorkId(work.getId()).isEmpty());
    }

    @Test
    void workLinkMapperKeepsContentOnSortOnlyUpdates() {
        WorkEntity work = insertWork();

        WorkLinkEntity link = new WorkLinkEntity();
        link.setWorkId(work.getId());
        link.setLinkType("GITHUB");
        link.setLabel("仓库");
        link.setUrl("https://example.test/repo");
        link.setSortOrder(1);
        link.setStatus("ENABLED");
        assertEquals(1, links.insert(link));
        assertNotNull(link.getId(), "insert 没有回填自增主键");

        WorkLinkEntity loaded = links.selectById(link.getId());
        assertNotNull(loaded);
        assertEquals("ENABLED", loaded.getStatus());
        assertEquals(1, links.listByWorkId(work.getId()).size());

        loaded.setLabel("仓库（改）");
        loaded.setUrl("https://example.test/repo-2");
        loaded.setStatus("DISABLED");
        loaded.setSortOrder(8);
        assertEquals(1, links.updateContent(loaded));
        WorkLinkEntity contentUpdated = links.selectById(link.getId());
        assertEquals("仓库（改）", contentUpdated.getLabel());
        assertEquals("DISABLED", contentUpdated.getStatus());
        assertEquals("GITHUB", contentUpdated.getLinkType(), "更新内容不应改动 link_type");
        assertEquals(work.getId(), contentUpdated.getWorkId());

        contentUpdated.setSortOrder(42);
        assertEquals(1, links.updateSortOrder(contentUpdated));
        WorkLinkEntity sorted = links.selectById(link.getId());
        assertEquals(42, sorted.getSortOrder().intValue());
        assertEquals("仓库（改）", sorted.getLabel(), "改排序不应动 label");
        assertEquals("https://example.test/repo-2", sorted.getUrl(), "改排序不应动 url");
        assertEquals("DISABLED", sorted.getStatus(), "改排序不应动 status");

        assertEquals(1, links.deleteById(link.getId()));
        assertNull(links.selectById(link.getId()));
        assertTrue(links.listByWorkId(work.getId()).isEmpty());
    }

    @Test
    void profileMapperUpdatesBasicAndMediaWithoutTouchingOtherColumns() {
        ProfileEntity original = profiles.selectOwner();
        assertNotNull(original, "sr_profile 里没有 profile_key = 'OWNER' 的行，检查 V2_013 迁移是否执行");

        String marker = unique("mapperxmlprofile");
        original.setDisplayName(marker);
        original.setHeadline("XML 验证标题");
        original.setBioMarkdown("XML 验证介绍");
        original.setLocationText("XML 验证所在地");
        original.setUpdatedAt(now());
        assertEquals(1, profiles.updateBasic(original));

        ProfileEntity basicUpdated = profiles.selectOwner();
        assertEquals(marker, basicUpdated.getDisplayName());
        assertEquals("XML 验证标题", basicUpdated.getHeadline());
        assertEquals("XML 验证介绍", basicUpdated.getBioMarkdown());
        assertEquals("XML 验证所在地", basicUpdated.getLocationText());
        assertEquals(original.getProfileKey(), basicUpdated.getProfileKey(), "改资料不应动 profile_key");
        assertEquals(original.getStatus(), basicUpdated.getStatus(), "改资料不应动 status");
        assertEquals(original.getAvatarMediaAssetId(), basicUpdated.getAvatarMediaAssetId(),
                "改资料不应动头像引用");
        assertEquals(original.getResumeMediaAssetId(), basicUpdated.getResumeMediaAssetId(),
                "改资料不应动简历引用");

        // 清空标题：headline 原先带 FieldStrategy.ALWAYS，null 必须真的写进库
        basicUpdated.setHeadline(null);
        basicUpdated.setUpdatedAt(now());
        assertEquals(1, profiles.updateBasic(basicUpdated));
        assertNull(profiles.selectOwner().getHeadline(),
                "headline 传 null 没有被清空，ALWAYS 更新语义在迁移中丢了");

        // 换头像：显示名称等文案必须保持原值，简历引用保持原值
        ProfileEntity beforeMedia = profiles.selectOwner();
        ProfileEntity mediaUpdated = profiles.selectOwner();
        long newAvatar = probeId();
        mediaUpdated.setAvatarMediaAssetId(newAvatar);
        mediaUpdated.setUpdatedAt(now());
        assertEquals(1, profiles.updateMediaAssets(mediaUpdated));
        ProfileEntity afterMedia = profiles.selectOwner();
        assertEquals(Long.valueOf(newAvatar), afterMedia.getAvatarMediaAssetId());
        assertEquals(beforeMedia.getDisplayName(), afterMedia.getDisplayName(), "换媒体不应改动 display_name");
        assertEquals(beforeMedia.getHeadline(), afterMedia.getHeadline(), "换媒体不应改动 headline");
        assertEquals(beforeMedia.getBioMarkdown(), afterMedia.getBioMarkdown(), "换媒体不应改动 bio_markdown");
        assertEquals(beforeMedia.getLocationText(), afterMedia.getLocationText(), "换媒体不应改动 location_text");
        assertEquals(beforeMedia.getResumeMediaAssetId(), afterMedia.getResumeMediaAssetId());

        // 解除头像引用：avatar 为 null 必须真的写进库（ALWAYS 语义）
        afterMedia.setAvatarMediaAssetId(null);
        afterMedia.setUpdatedAt(now());
        assertEquals(1, profiles.updateMediaAssets(afterMedia));
        assertNull(profiles.selectOwner().getAvatarMediaAssetId(),
                "avatarMediaAssetId 传 null 没有被清空，ALWAYS 更新语义在迁移中丢了");
    }

    @Test
    void experienceMapperRoundTripsContentSortAndClearableColumns() {
        long profileId = probeId();
        ExperienceEntity row = newExperience(profileId, unique("mapperxmlexp"));
        assertEquals(1, experiences.insert(row));
        assertNotNull(row.getId(), "insert 没有回填自增主键");

        ExperienceEntity loaded = experiences.selectById(row.getId());
        assertNotNull(loaded);
        assertEquals("ENABLED", loaded.getStatus());
        assertEquals(0, loaded.getSortOrder().intValue());
        assertEquals(LocalDate.of(2024, 1, 1), loaded.getStartDate());
        assertNotNull(loaded.getCreatedAt());
        assertEquals(1, experiences.listByProfileId(profileId).size());
        assertEquals(1, experiences.listEnabledByProfileId(profileId).size());
        assertEquals(1, experiences.countByProfileId(profileId));
        assertEquals(0, experiences.countByProfileId(probeId()));

        // organization / end_date / description_md 是「允许被清空」的列（旧 ALWAYS 语义）
        loaded.setTitle("改名后的经历");
        loaded.setOrganization(null);
        loaded.setEndDate(null);
        loaded.setDescriptionMd(null);
        loaded.setUpdatedAt(now());
        assertEquals(1, experiences.updateContent(loaded));
        ExperienceEntity contentUpdated = experiences.selectById(row.getId());
        assertEquals("改名后的经历", contentUpdated.getTitle());
        assertNull(contentUpdated.getOrganization(), "organization 传 null 没有被清空");
        assertNull(contentUpdated.getEndDate(), "end_date 传 null 没有被清空");
        assertNull(contentUpdated.getDescriptionMd(), "description_md 传 null 没有被清空");
        assertEquals("CAREER", contentUpdated.getExperienceType(), "更新内容不应改动类型");
        assertEquals(LocalDate.of(2024, 1, 1), contentUpdated.getStartDate(), "更新内容不应改动开始日期");
        assertEquals(profileId, contentUpdated.getProfileId(), "更新内容不应改动归属 profile");
        assertEquals("ENABLED", contentUpdated.getStatus(), "更新内容不应改动 status");
        assertEquals(Boolean.FALSE, contentUpdated.getIsCurrent());

        contentUpdated.setSortOrder(33);
        contentUpdated.setUpdatedAt(now());
        assertEquals(1, experiences.updateSortOrder(contentUpdated));
        ExperienceEntity sorted = experiences.selectById(row.getId());
        assertEquals(33, sorted.getSortOrder().intValue());
        assertEquals("改名后的经历", sorted.getTitle(), "改排序不应动 title");
        assertNull(sorted.getOrganization(), "改排序不应把已清空的 organization 写回来");

        assertEquals(1, experiences.deleteById(row.getId()));
        assertNull(experiences.selectById(row.getId()));
        assertTrue(experiences.listByProfileId(profileId).isEmpty());
    }

    @Test
    void skillMapperRoundTripsContentSortAndClearableColumns() {
        long profileId = probeId();
        SkillEntity row = newSkill(profileId, unique("mapperxmlskill"));
        assertEquals(1, skills.insert(row));
        assertNotNull(row.getId(), "insert 没有回填自增主键");

        SkillEntity loaded = skills.selectById(row.getId());
        assertNotNull(loaded);
        assertEquals("SKILL", loaded.getCategory());
        assertEquals("ENABLED", loaded.getStatus());
        assertEquals(1, skills.listByProfileId(profileId).size());
        assertEquals(1, skills.listEnabledByProfileId(profileId).size());
        assertEquals(1, skills.countByProfileId(profileId));

        loaded.setDescription(null);
        loaded.setProficiency(null);
        loaded.setName("改名后的技能");
        loaded.setUpdatedAt(now());
        assertEquals(1, skills.updateContent(loaded));
        SkillEntity contentUpdated = skills.selectById(row.getId());
        assertEquals("改名后的技能", contentUpdated.getName());
        assertNull(contentUpdated.getDescription(), "description 传 null 没有被清空");
        assertNull(contentUpdated.getProficiency(), "proficiency 传 null 没有被清空");
        assertEquals("SKILL", contentUpdated.getCategory(), "更新内容不应改动 category");
        assertEquals(profileId, contentUpdated.getProfileId(), "更新内容不应改动归属 profile");

        contentUpdated.setSortOrder(12);
        contentUpdated.setUpdatedAt(now());
        assertEquals(1, skills.updateSortOrder(contentUpdated));
        SkillEntity sorted = skills.selectById(row.getId());
        assertEquals(12, sorted.getSortOrder().intValue());
        assertEquals("改名后的技能", sorted.getName(), "改排序不应动 name");
        assertNull(sorted.getDescription(), "改排序不应把已清空的 description 写回来");

        assertEquals(1, skills.deleteById(row.getId()));
        assertNull(skills.selectById(row.getId()));
    }

    @Test
    void socialLinkMapperRoundTripsContentAndSort() {
        long profileId = probeId();
        SocialLinkEntity row = newSocial(profileId);
        assertEquals(1, socials.insert(row));
        assertNotNull(row.getId(), "insert 没有回填自增主键");

        SocialLinkEntity loaded = socials.selectById(row.getId());
        assertNotNull(loaded);
        assertEquals("ENABLED", loaded.getStatus());
        assertEquals(1, socials.listByProfileId(profileId).size());
        assertEquals(1, socials.listEnabledByProfileId(profileId).size());
        assertEquals(1, socials.countByProfileId(profileId));

        loaded.setLabel("改名后的链接");
        loaded.setUrl("https://example.test/moved");
        loaded.setUpdatedAt(now());
        assertEquals(1, socials.updateContent(loaded));
        SocialLinkEntity contentUpdated = socials.selectById(row.getId());
        assertEquals("改名后的链接", contentUpdated.getLabel());
        assertEquals("https://example.test/moved", contentUpdated.getUrl());
        assertEquals("OTHER", contentUpdated.getPlatformCode(), "更新内容不应改动 platform_code");
        assertEquals(profileId, contentUpdated.getProfileId(), "更新内容不应改动归属 profile");

        contentUpdated.setSortOrder(21);
        contentUpdated.setUpdatedAt(now());
        assertEquals(1, socials.updateSortOrder(contentUpdated));
        SocialLinkEntity sorted = socials.selectById(row.getId());
        assertEquals(21, sorted.getSortOrder().intValue());
        assertEquals("改名后的链接", sorted.getLabel(), "改排序不应动 label");
        assertEquals("https://example.test/moved", sorted.getUrl(), "改排序不应动 url");
        assertEquals("ENABLED", sorted.getStatus(), "改排序不应动 status");

        assertEquals(1, socials.deleteById(row.getId()));
        assertNull(socials.selectById(row.getId()));
    }

    @Test
    void featuredContentMapperRoundTripsOrderAndDelete() {
        long profileId = probeId();
        long contentId = probeId();
        FeaturedContentEntity row = new FeaturedContentEntity();
        row.setProfileId(profileId);
        row.setContentType("PORTFOLIO");
        row.setContentId(contentId);
        row.setTitleOverride("精选标题");
        row.setSortOrder(0);
        row.setStatus("ENABLED");
        row.setCreatedAt(now());
        row.setUpdatedAt(now());
        assertEquals(1, featured.insert(row));
        assertNotNull(row.getId(), "insert 没有回填自增主键");

        FeaturedContentEntity loaded = featured.selectById(row.getId());
        assertNotNull(loaded);
        assertEquals("精选标题", loaded.getTitleOverride());
        assertEquals(1, featured.listByProfileId(profileId).size());
        assertEquals(1, featured.listEnabledByProfileId(profileId).size());
        assertEquals(1, featured.countByProfileId(profileId));

        loaded.setSortOrder(4);
        loaded.setUpdatedAt(now());
        assertEquals(1, featured.updateSortOrder(loaded));
        FeaturedContentEntity sorted = featured.selectById(row.getId());
        assertEquals(4, sorted.getSortOrder().intValue());
        assertEquals("PORTFOLIO", sorted.getContentType(), "改排序不应动 content_type");
        assertEquals(contentId, sorted.getContentId(), "改排序不应动 content_id");
        assertEquals("精选标题", sorted.getTitleOverride(), "改排序不应动 title_override");
        assertEquals(profileId, sorted.getProfileId(), "改排序不应动归属 profile");

        assertEquals(1, featured.deleteById(row.getId()));
        assertNull(featured.selectById(row.getId()));
        assertEquals(0, featured.countByProfileId(profileId));
    }

    @Test
    void englishOverviewMapperReadsAndUpdatesTheSingleton() {
        EnglishOverviewEntity original = englishOverview.selectSingleton();
        assertNotNull(original, "sr_english_overview 没有 id = 1 的行，检查 V2_019 迁移是否执行");
        assertEquals(1, original.getId().intValue());
        LocalDateTime originalCreatedAt = original.getCreatedAt();

        assertEquals(1, englishOverview.updateContent("XML 验证标题", "XML 验证副标题", null, null));

        EnglishOverviewEntity updated = englishOverview.selectSingleton();
        assertEquals("XML 验证标题", updated.getTitle());
        assertEquals("XML 验证副标题", updated.getSubtitle());
        assertNull(updated.getIntroduction(), "introduction 传 null 没有被清空");
        assertNull(updated.getRoadmapMarkdown(), "roadmap_markdown 传 null 没有被清空");
        assertEquals(original.getCurrentStage(), updated.getCurrentStage(),
                "改文案不应改动 current_stage");
        assertEquals(1, updated.getId().intValue());
        assertEquals(originalCreatedAt, updated.getCreatedAt(), "改文案不应改动 created_at");
        assertNotNull(updated.getUpdatedAt());
    }

    @Test
    void rollbackLeavesNoTestRowsAndRestoresTheOwnerProfile() {
        ProfileEntity beforeUpdate = profiles.selectOwner();
        assertNotNull(beforeUpdate);
        String originalDisplayName = beforeUpdate.getDisplayName();
        String originalHeadline = beforeUpdate.getHeadline();
        String originalBio = beforeUpdate.getBioMarkdown();
        String originalLocation = beforeUpdate.getLocationText();
        Long originalAvatar = beforeUpdate.getAvatarMediaAssetId();
        Long originalResume = beforeUpdate.getResumeMediaAssetId();

        beforeUpdate.setDisplayName(unique("mapperxmlrollback"));
        beforeUpdate.setHeadline("XML 回滚标题");
        beforeUpdate.setBioMarkdown("XML 回滚介绍");
        beforeUpdate.setLocationText("XML 回滚所在地");
        beforeUpdate.setAvatarMediaAssetId(probeId());
        beforeUpdate.setUpdatedAt(now());
        assertEquals(1, profiles.updateBasic(beforeUpdate));
        assertEquals(1, profiles.updateMediaAssets(beforeUpdate));

        WorkEntity work = insertWork();
        ExperienceEntity experience = newExperience(probeId(), unique("mapperxmlrollback-exp"));
        assertEquals(1, experiences.insert(experience));

        session.rollback();
        try (SqlSession fresh = openSession()) {
            assertNull(fresh.getMapper(WorkMapper.class).selectById(work.getId()),
                    "回滚后不应在开发库里留下测试作品");
            assertNull(fresh.getMapper(ExperienceMapper.class).selectById(experience.getId()),
                    "回滚后不应在开发库里留下测试经历");
            ProfileEntity after = fresh.getMapper(ProfileMapper.class).selectOwner();
            assertNotNull(after);
            assertEquals(originalDisplayName, after.getDisplayName(), "回滚后 OWNER 显示名称应当还原");
            assertEquals(originalHeadline, after.getHeadline(), "回滚后 OWNER 标题应当还原");
            assertEquals(originalBio, after.getBioMarkdown(), "回滚后 OWNER 介绍应当还原");
            assertEquals(originalLocation, after.getLocationText(), "回滚后 OWNER 所在地应当还原");
            assertEquals(originalAvatar, after.getAvatarMediaAssetId(), "回滚后 OWNER 头像引用应当还原");
            assertEquals(originalResume, after.getResumeMediaAssetId(), "回滚后 OWNER 简历引用应当还原");
        }
    }

    private WorkEntity insertWork() {
        WorkEntity work = newWork(unique("mapperxmlwork"), "XML 验证作品", "OTHER", "DRAFT");
        assertEquals(1, works.insert(work));
        return work;
    }

    private WorkEntity newWork(String slug, String title, String workType, String status) {
        WorkEntity work = new WorkEntity();
        work.setSlug(slug);
        work.setWorkType(workType);
        work.setTitle(title);
        work.setSummary("摘要");
        work.setBodyMarkdown("# 正文");
        work.setStatus(status);
        work.setCreatedByAccountId(probeId());
        work.setUpdatedByAccountId(work.getCreatedByAccountId());
        return work;
    }

    private ExperienceEntity newExperience(long profileId, String title) {
        ExperienceEntity row = new ExperienceEntity();
        row.setProfileId(profileId);
        row.setExperienceType("CAREER");
        row.setTitle(title);
        row.setOrganization("XML 验证机构");
        row.setStartDate(LocalDate.of(2024, 1, 1));
        row.setEndDate(LocalDate.of(2024, 12, 31));
        row.setIsCurrent(Boolean.FALSE);
        row.setDescriptionMd("XML 验证经历说明");
        row.setSortOrder(0);
        row.setStatus("ENABLED");
        row.setCreatedAt(now());
        row.setUpdatedAt(now());
        return row;
    }

    private SkillEntity newSkill(long profileId, String name) {
        SkillEntity row = new SkillEntity();
        row.setProfileId(profileId);
        row.setCategory("SKILL");
        row.setName(name);
        row.setDescription("XML 验证说明");
        row.setProficiency("熟练");
        row.setSortOrder(0);
        row.setStatus("ENABLED");
        row.setCreatedAt(now());
        row.setUpdatedAt(now());
        return row;
    }

    private SocialLinkEntity newSocial(long profileId) {
        SocialLinkEntity row = new SocialLinkEntity();
        row.setProfileId(profileId);
        row.setPlatformCode("OTHER");
        row.setLabel("XML 验证链接");
        row.setUrl("https://example.test/link");
        row.setSortOrder(0);
        row.setStatus("ENABLED");
        row.setCreatedAt(now());
        row.setUpdatedAt(now());
        return row;
    }

    /* 逻辑外键，库里没有物理 FOREIGN KEY：用一个大号段避免和真实账户/资料撞号 */
    private long probeId() {
        return 900_000_000_000L + PROBE.incrementAndGet();
    }

    private String unique(String prefix) {
        return prefix + "-" + PROBE.incrementAndGet() + "-" + System.nanoTime();
    }

    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    }
}
