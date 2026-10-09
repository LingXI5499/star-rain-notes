package com.starrainnotes.boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.starrainnotes.site.entity.HomeSectionEntity;
import com.starrainnotes.site.entity.SiteConfigEntity;
import com.starrainnotes.site.mapper.HomeSectionMapper;
import com.starrainnotes.site.mapper.SiteConfigMapper;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/*
 * site 模块 2 个 Mapper（SiteConfigMapper、HomeSectionMapper）的真实数据库读写验证。
 *
 * 这两个 Mapper 都是「单例 / 少量种子行 + 全量写回」的形态，且没有任何插入语句：
 *   sr_site_config 只有 config_key = 'PRIMARY' 一行，update 按主键全量覆盖 8 个文案列；
 *   sr_site_home_section 有 7 行种子，update 覆盖 display_name/enabled/sort_order/config_json。
 * 单元测试里 Mapper 被 mock 掉，所以「改排序时会不会顺手把别的列写坏」「可空列传 null
 * 会不会真的写成 NULL」「JSON 列能不能按字符串读回」这些都没被验证过。
 *
 * 本类会临时改写真实站点文案与首页区块，全部在**未提交事务**里做，@AfterEach 回滚；
 * 最后一个用例用新连接逐字段复核 OWNER 行已回到测试前的值。
 *
 * 这两个 Mapper 都没有 insert / delete 语句，所以只测读路径与两条 update 写路径。
 */
class SiteMapperXmlTest extends MapperXmlIntegrationSupport {

    private static final AtomicLong PROBE = new AtomicLong(System.nanoTime() % 1_000_000L);

    private SqlSession session;
    private SiteConfigMapper configs;
    private HomeSectionMapper sections;

    @BeforeEach
    void open() {
        session = openSession();
        configs = session.getMapper(SiteConfigMapper.class);
        sections = session.getMapper(HomeSectionMapper.class);
    }

    @AfterEach
    void rollback() {
        if (session != null) {
            session.rollback();
            session.close();
        }
    }

    @Test
    void everySiteMapperIsReachableThroughTheSession() {
        assertNotNull(configs);
        assertNotNull(sections);
    }

    @Test
    void primaryConfigReadsTheSingletonRowWithEveryColumn() {
        SiteConfigEntity config = configs.primary();
        assertNotNull(config, "sr_site_config 里没有 config_key = 'PRIMARY' 的行，检查 V2_017 迁移");
        assertEquals("PRIMARY", config.getConfigKey());
        assertNotNull(config.getId());
        assertNotNull(config.getSiteName(), "site_name 是 NOT NULL 列，必须能读回");
        assertNotNull(config.getSiteTitle());
        assertNotNull(config.getCreatedAt(), "created_at 由列默认值填充，应能读回");
        assertNotNull(config.getUpdatedAt());
    }

    @Test
    void configUpdateWritesEveryCopyColumnAndKeepsConfigKeyAndId() {
        SiteConfigEntity original = configs.primary();
        assertNotNull(original);
        String marker = unique("mapperxmlsite");
        long logo = probeId();

        SiteConfigEntity edited = configs.primary();
        edited.setSiteName(marker);
        edited.setSiteTitle(marker + "-标题");
        edited.setTagline(marker + "-标语");
        edited.setSiteDescription(marker + "-描述");
        edited.setHomeIntro(marker + "-首页介绍");
        edited.setFooterText(marker + "-页脚");
        edited.setLogoMediaAssetId(logo);
        edited.setFaviconMediaAssetId(probeId());
        edited.setUpdatedAt(now());
        assertEquals(1, configs.update(edited));

        SiteConfigEntity updated = configs.primary();
        assertEquals(marker, updated.getSiteName());
        assertEquals(marker + "-标题", updated.getSiteTitle());
        assertEquals(marker + "-标语", updated.getTagline());
        assertEquals(marker + "-描述", updated.getSiteDescription());
        assertEquals(marker + "-首页介绍", updated.getHomeIntro());
        assertEquals(marker + "-页脚", updated.getFooterText());
        assertEquals(Long.valueOf(logo), updated.getLogoMediaAssetId());
        assertEquals(edited.getFaviconMediaAssetId(), updated.getFaviconMediaAssetId());
        assertEquals(original.getId(), updated.getId(), "改文案不应改动主键");
        assertEquals("PRIMARY", updated.getConfigKey(), "改文案不应改动 config_key");
        assertEquals(original.getCreatedAt(), updated.getCreatedAt(), "改文案不应改动 created_at");
        assertEquals(0, configs.update(withId(-1L, edited)), "不存在的 id 不应更新成功");
    }

    @Test
    void configUpdateWritesNullForEveryOptionalCopyColumn() {
        SiteConfigEntity original = configs.primary();
        assertNotNull(original);

        SiteConfigEntity cleared = configs.primary();
        cleared.setTagline(null);
        cleared.setSiteDescription(null);
        cleared.setHomeIntro(null);
        cleared.setFooterText(null);
        cleared.setLogoMediaAssetId(null);
        cleared.setFaviconMediaAssetId(null);
        cleared.setUpdatedAt(now());
        assertEquals(1, configs.update(cleared));

        SiteConfigEntity updated = configs.primary();
        // update 是普通 SET（没有 COALESCE），可空文案传 null 就是清空
        assertNull(updated.getTagline(), "tagline 传 null 没有被清空");
        assertNull(updated.getSiteDescription(), "site_description 传 null 没有被清空");
        assertNull(updated.getHomeIntro(), "home_intro 传 null 没有被清空");
        assertNull(updated.getFooterText(), "footer_text 传 null 没有被清空");
        assertNull(updated.getLogoMediaAssetId(), "logo_media_asset_id 传 null 没有被清空");
        assertNull(updated.getFaviconMediaAssetId(), "favicon_media_asset_id 传 null 没有被清空");
        // 非空列必须保持原值（不会被 null 覆盖成非法状态）
        assertNotNull(updated.getSiteName());
        assertNotNull(updated.getSiteTitle());
    }

    @Test
    void homeSectionsReadBackOrderedBySortOrderThenId() {
        List<HomeSectionEntity> all = sections.all();
        assertNotNull(all);
        assertTrue(all.size() >= 7, "首页区块种子数据看起来少了，检查 V2_017/V2_018 迁移");
        for (int index = 1; index < all.size(); index++) {
            HomeSectionEntity previous = all.get(index - 1);
            HomeSectionEntity current = all.get(index);
            assertTrue(previous.getSortOrder() < current.getSortOrder()
                            || (previous.getSortOrder().equals(current.getSortOrder())
                            && previous.getId() <= current.getId()),
                    "all() 必须按 sort_order,id 升序：" + previous.getSectionCode() + " / " + current.getSectionCode());
        }

        HomeSectionEntity hero = sections.byCode("HERO");
        assertNotNull(hero, "sr_site_home_section 里没有 section_code = 'HERO' 的行");
        assertEquals("HERO", hero.getSectionCode());
        assertNotNull(hero.getDisplayName(), "display_name 是 NOT NULL 列，必须能读回");
        assertNotNull(hero.getEnabled(), "enabled 是 tinyint(1) NOT NULL，必须映射成 Boolean");
        assertNotNull(hero.getSortOrder());
        assertNotNull(hero.getCreatedAt(), "created_at 由列默认值填充，应能读回");

        assertNull(sections.byCode(unique("mapperxmlmissing-section")), "不存在的 section_code 应返回 null");
    }

    @Test
    void homeSectionUpdateWritesOnlyItsOwnColumns() {
        HomeSectionEntity original = sections.byCode("HERO");
        assertNotNull(original);
        String marker = unique("mapperxmlsection");

        HomeSectionEntity edited = sections.byCode("HERO");
        edited.setDisplayName(marker);
        edited.setEnabled(Boolean.FALSE);
        edited.setSortOrder(999);
        edited.setConfigJson("{\"marker\":\"" + marker + "\"}");
        edited.setUpdatedAt(now());
        assertEquals(1, sections.update(edited));

        HomeSectionEntity updated = sections.byCode("HERO");
        assertEquals(marker, updated.getDisplayName(), "display_name 没有被更新");
        assertEquals(Boolean.FALSE, updated.getEnabled(), "enabled 没有被更新成 false");
        assertEquals(999, updated.getSortOrder().intValue(), "sort_order 没有被更新");
        assertTrue(updated.getConfigJson().contains(marker), "config_json 没有按字符串写回并读回");
        assertEquals(original.getId(), updated.getId(), "改文案不应改动主键");
        assertEquals("HERO", updated.getSectionCode(), "改文案不应改动 section_code");
        assertEquals(original.getCreatedAt(), updated.getCreatedAt(), "改文案不应改动 created_at");

        HomeSectionEntity reEnabled = sections.byCode("HERO");
        reEnabled.setEnabled(Boolean.TRUE);
        reEnabled.setConfigJson(null);
        reEnabled.setUpdatedAt(now());
        assertEquals(1, sections.update(reEnabled));
        HomeSectionEntity back = sections.byCode("HERO");
        assertEquals(Boolean.TRUE, back.getEnabled(), "enabled 没有被更新成 true");
        assertNull(back.getConfigJson(), "config_json 传 null 没有被清空");

        assertEquals(0, sections.update(withSectionId(-1L, reEnabled)), "不存在的 id 不应更新成功");
    }

    @Test
    void rollbackRestoresThePrimaryConfigAndTheHomeSections() {
        SiteConfigEntity before = configs.primary();
        assertNotNull(before);
        HomeSectionEntity heroBefore = sections.byCode("HERO");
        assertNotNull(heroBefore);
        String originalName = before.getSiteName();
        String originalTitle = before.getSiteTitle();
        String originalTagline = before.getTagline();
        String originalDescription = before.getSiteDescription();
        String originalHomeIntro = before.getHomeIntro();
        String originalFooter = before.getFooterText();
        Long originalLogo = before.getLogoMediaAssetId();
        Long originalFavicon = before.getFaviconMediaAssetId();
        String originalSectionName = heroBefore.getDisplayName();
        Boolean originalSectionEnabled = heroBefore.getEnabled();
        Integer originalSectionOrder = heroBefore.getSortOrder();
        String originalSectionConfig = heroBefore.getConfigJson();
        int sectionCount = sections.all().size();

        SiteConfigEntity edited = configs.primary();
        edited.setSiteName(unique("mapperxmlrollback"));
        edited.setSiteTitle("XML 回滚标题");
        edited.setTagline(null);
        edited.setSiteDescription(null);
        edited.setHomeIntro(null);
        edited.setFooterText(null);
        edited.setLogoMediaAssetId(probeId());
        edited.setFaviconMediaAssetId(probeId());
        edited.setUpdatedAt(now());
        assertEquals(1, configs.update(edited));

        HomeSectionEntity section = sections.byCode("HERO");
        section.setDisplayName("XML 回滚区块");
        section.setEnabled(Boolean.FALSE);
        section.setSortOrder(12345);
        section.setConfigJson("{\"rollback\":true}");
        section.setUpdatedAt(now());
        assertEquals(1, sections.update(section));

        session.rollback();
        try (SqlSession fresh = openSession()) {
            SiteConfigEntity after = fresh.getMapper(SiteConfigMapper.class).primary();
            assertNotNull(after);
            assertEquals(originalName, after.getSiteName(), "回滚后 site_name 应当还原");
            assertEquals(originalTitle, after.getSiteTitle(), "回滚后 site_title 应当还原");
            assertEquals(originalTagline, after.getTagline(), "回滚后 tagline 应当还原");
            assertEquals(originalDescription, after.getSiteDescription(), "回滚后 site_description 应当还原");
            assertEquals(originalHomeIntro, after.getHomeIntro(), "回滚后 home_intro 应当还原");
            assertEquals(originalFooter, after.getFooterText(), "回滚后 footer_text 应当还原");
            assertEquals(originalLogo, after.getLogoMediaAssetId(), "回滚后 logo 引用应当还原");
            assertEquals(originalFavicon, after.getFaviconMediaAssetId(), "回滚后 favicon 引用应当还原");

            HomeSectionMapper freshSections = fresh.getMapper(HomeSectionMapper.class);
            HomeSectionEntity heroAfter = freshSections.byCode("HERO");
            assertNotNull(heroAfter);
            assertEquals(originalSectionName, heroAfter.getDisplayName(), "回滚后区块名称应当还原");
            assertEquals(originalSectionEnabled, heroAfter.getEnabled(), "回滚后区块开关应当还原");
            assertEquals(originalSectionOrder, heroAfter.getSortOrder(), "回滚后区块排序应当还原");
            assertEquals(originalSectionConfig, heroAfter.getConfigJson(), "回滚后区块配置应当还原");
            assertEquals(sectionCount, freshSections.all().size(), "回滚后区块行数不应变化");
        }
    }

    private SiteConfigEntity withId(Long id, SiteConfigEntity source) {
        SiteConfigEntity copy = new SiteConfigEntity();
        copy.setId(id);
        copy.setConfigKey(source.getConfigKey());
        copy.setSiteName(source.getSiteName());
        copy.setSiteTitle(source.getSiteTitle());
        copy.setTagline(source.getTagline());
        copy.setSiteDescription(source.getSiteDescription());
        copy.setHomeIntro(source.getHomeIntro());
        copy.setFooterText(source.getFooterText());
        copy.setLogoMediaAssetId(source.getLogoMediaAssetId());
        copy.setFaviconMediaAssetId(source.getFaviconMediaAssetId());
        copy.setUpdatedAt(source.getUpdatedAt());
        return copy;
    }

    private HomeSectionEntity withSectionId(Long id, HomeSectionEntity source) {
        HomeSectionEntity copy = new HomeSectionEntity();
        copy.setId(id);
        copy.setSectionCode(source.getSectionCode());
        copy.setDisplayName(source.getDisplayName());
        copy.setEnabled(source.getEnabled());
        copy.setSortOrder(source.getSortOrder());
        copy.setConfigJson(source.getConfigJson());
        copy.setUpdatedAt(source.getUpdatedAt());
        return copy;
    }

    /* 逻辑外键，库里没有物理 FOREIGN KEY：用一个大号段避免和真实媒体资源撞号 */
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
