/*
 * MediaMapperXmlTest：应用启动模块的测试文件。
 * 业务范围：多模块装配、入口路由、安全配置和启动初始化。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：MediaAssetEntity、MediaReferenceEntity、MediaAssetMapper、MediaReferenceMapper。
 * 阅读入口：open（打开媒体数据映射Xml）；rollback（回滚媒体数据映射Xml）；everyMediaMapperIsReachableThroughTheSession（every媒体数据映射是否ReachableThroughThe会话处理入口）；insertedAssetIsReadableBackWithEveryColumn（inserted资产是否ReadableBack包含Every列处理入口）。
 */

package com.starrainnotes.boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.starrainnotes.media.entity.MediaAssetEntity;
import com.starrainnotes.media.entity.MediaReferenceEntity;
import com.starrainnotes.media.mapper.MediaAssetMapper;
import com.starrainnotes.media.mapper.MediaReferenceMapper;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/*
 * media 模块 2 个 Mapper（MediaAssetMapper、MediaReferenceMapper）的真实数据库读写验证。
 *
 * 这两个 Mapper 也刚从 BaseMapper 迁到「接口显式方法 + XML」，单元测试把 Mapper 整个 mock 掉，
 * 所以 XML 的列清单、自增主键回填、`<where>` 拼出来的四个可选筛选、状态守卫
 * （archive 只在 ACTIVE 生效、restore 只在 ARCHIVED 生效）都没被验证过。
 *
 * 特别值得测的是 assetPage 的四段 `<if>`：accessLevel 与 mediaType 是最容易漏写的两个，
 * 漏了会变成「筛选不生效、后台列表越筛越多」，而不会有任何异常。
 *
 * 全程在一个不 commit 的 SqlSession 里跑，@AfterEach 回滚；最后一个用例用新连接复核。
 */
/**
 * 媒体数据映射Xml测试，属于应用启动模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class MediaMapperXmlTest extends MapperXmlIntegrationSupport {

    /**
     * 测试样本。
     */
    private static final AtomicLong PROBE = new AtomicLong(System.nanoTime() % 1_000_000L);

    /**
     * 当前业务使用的会话对象。
     */
    private SqlSession session;
    /**
     * assets。
     * 协作类型：<code>MediaAssetMapper</code>。
     */
    private MediaAssetMapper assets;
    /**
     * references。
     * 协作类型：<code>MediaReferenceMapper</code>。
     */
    private MediaReferenceMapper references;

    /**
     * 打开媒体数据映射Xml。
     * 主要协作调用：<code>session.getMapper</code>。
     */
    @BeforeEach
    void open() {
        session = openSession();
        assets = session.getMapper(MediaAssetMapper.class);
        references = session.getMapper(MediaReferenceMapper.class);
    }

    /**
     * 回滚媒体数据映射Xml。
     * 主要协作调用：<code>session.rollback</code>、<code>session.close</code>。
     */
    @AfterEach
    void rollback() {
        if (session != null) {
            session.rollback();
            session.close();
        }
    }

    /**
     * every媒体数据映射是否ReachableThroughThe会话处理入口。
     */
    @Test
    void everyMediaMapperIsReachableThroughTheSession() {
        assertNotNull(assets);
        assertNotNull(references);
    }

    /**
     * inserted资产是否ReadableBack包含Every列处理入口。
     * 主要协作调用：<code>assets.insertAsset</code>、<code>assets.assetById</code>、<code>assets.assetByIdForUpdate</code>、<code>assets.countBySha256</code>、<code>assets.allStorageKeys</code>。
     */
    @Test
    void insertedAssetIsReadableBackWithEveryColumn() {
        MediaAssetEntity asset = newAsset(unique("mapperxmlmedia") + "-image.png", "IMAGE", "PUBLIC");
        assets.insertAsset(asset);
        assertNotNull(asset.getId(), "insertAsset 没有把自增主键回填到实体上");

        MediaAssetEntity loaded = assets.assetById(asset.getId());
        assertNotNull(loaded, "assetById 读不到刚插入的行");
        assertEquals(asset.getOriginalName(), loaded.getOriginalName());
        assertEquals("IMAGE", loaded.getMediaType());
        assertEquals("image/png", loaded.getMimeType());
        assertEquals("png", loaded.getFileExtension());
        assertEquals(asset.getSizeBytes(), loaded.getSizeBytes());
        assertEquals(asset.getSha256(), loaded.getSha256());
        assertEquals("LOCAL", loaded.getStorageProvider());
        assertEquals(asset.getStorageKey(), loaded.getStorageKey());
        assertEquals(1920, loaded.getWidth().intValue(), "width 没有映射回来");
        assertEquals(1080, loaded.getHeight().intValue(), "height 没有映射回来");
        assertEquals("PUBLIC", loaded.getAccessLevel());
        assertEquals("ACTIVE", loaded.getStatus());
        assertEquals(asset.getUploadedByAccountId(), loaded.getUploadedByAccountId());
        assertNull(loaded.getArchivedAt());
        assertNotNull(loaded.getCreatedAt(), "created_at 由列默认值填充，应能读回");
        assertNotNull(loaded.getUpdatedAt());

        assertEquals(asset.getId(), assets.assetByIdForUpdate(asset.getId()).getId(),
                "FOR UPDATE 读不到刚插入的行");
        assertNull(assets.assetById(-1L), "不存在的 id 应返回 null");

        assertEquals(1, assets.countBySha256(asset.getSha256()));
        assertEquals(0, assets.countBySha256(token64(unique("mapperxmlmissingsha"))),
                "sha256 查重条件没生效");
        assertTrue(assets.allStorageKeys().contains(asset.getStorageKey()),
                "孤儿清理用的全量 storage_key 列表里找不到刚插入的键");
    }

    /**
     * 资产分页Honours全部FourOptional筛选处理入口。
     * 主要协作调用：<code>assets.insertAsset</code>、<code>assets.assetPageCount</code>、<code>assets.assetPage</code>。
     */
    @Test
    void assetPageHonoursAllFourOptionalFilters() {
        String keyword = unique("mapperxmlmediapage");
        MediaAssetEntity image = newAsset(keyword + "-image.png", "IMAGE", "PUBLIC");
        assets.insertAsset(image);
        MediaAssetEntity document = newAsset(keyword + "-doc.pdf", "DOCUMENT", "PROTECTED");
        assets.insertAsset(document);

        assertEquals(2, assets.assetPageCount(keyword, null, null, null),
                "keyword 过滤（original_name LIKE）没生效");
        assertEquals(1, assets.assetPageCount(keyword, "IMAGE", null, null), "mediaType 过滤没生效");
        assertEquals(1, assets.assetPageCount(keyword, "DOCUMENT", null, null));
        assertEquals(0, assets.assetPageCount(keyword, "VIDEO", null, null));
        assertEquals(2, assets.assetPageCount(keyword, null, "ACTIVE", null));
        assertEquals(0, assets.assetPageCount(keyword, null, "ARCHIVED", null), "status 过滤没生效");
        assertEquals(1, assets.assetPageCount(keyword, null, null, "PUBLIC"), "accessLevel 过滤没生效");
        assertEquals(1, assets.assetPageCount(keyword, null, null, "PROTECTED"));
        assertEquals(0, assets.assetPageCount(keyword + "-missing", null, null, null));

        List<MediaAssetEntity> all = assets.assetPage(keyword, null, null, null, 0, 10);
        assertEquals(2, all.size());
        assertEquals(1, assets.assetPage(keyword, null, null, null, 0, 1).size(), "LIMIT 1 没生效");
        assertEquals(1, assets.assetPage(keyword, null, null, null, 1, 1).size(), "OFFSET 没生效");
        Long firstId = assets.assetPage(keyword, null, null, null, 0, 1).get(0).getId();
        Long secondId = assets.assetPage(keyword, null, null, null, 1, 1).get(0).getId();
        assertTrue(!firstId.equals(secondId), "OFFSET 取到了同一行");
        assertEquals(1, assets.assetPage(keyword, "DOCUMENT", null, null, 0, 10).size());
        assertEquals(document.getId(), assets.assetPage(keyword, "DOCUMENT", null, null, 0, 10).get(0).getId());
    }

    /**
     * archive及RestoreFollowTheir状态Guards处理入口。
     * 主要协作调用：<code>assets.insertAsset</code>、<code>assets.archiveAsset</code>、<code>assets.assetById</code>、<code>assets.restoreAsset</code>、<code>assets.updateAccessLevel</code>。
     */
    @Test
    void archiveAndRestoreFollowTheirStateGuards() {
        MediaAssetEntity asset = newAsset(unique("mapperxmlmedia") + "-lifecycle.png", "IMAGE", "PUBLIC");
        assets.insertAsset(asset);
        LocalDateTime at = now();

        assertEquals(1, assets.archiveAsset(asset.getId(), at), "ACTIVE → ARCHIVED 应当成功");
        MediaAssetEntity archived = assets.assetById(asset.getId());
        assertEquals("ARCHIVED", archived.getStatus());
        assertNotNull(archived.getArchivedAt(), "archiveAsset 没有写入归档时间");
        assertEquals(0, assets.archiveAsset(asset.getId(), at), "已归档的不能重复归档");

        assertEquals(1, assets.restoreAsset(asset.getId()), "ARCHIVED → ACTIVE 应当成功");
        MediaAssetEntity restored = assets.assetById(asset.getId());
        assertEquals("ACTIVE", restored.getStatus());
        assertNull(restored.getArchivedAt(), "restoreAsset 必须把 archived_at 清成 NULL");
        assertEquals(0, assets.restoreAsset(asset.getId()), "已激活的不能重复恢复");

        assertEquals(1, assets.updateAccessLevel(asset.getId(), "PROTECTED"));
        assertEquals("PROTECTED", assets.assetById(asset.getId()).getAccessLevel());
        assertEquals("ACTIVE", assets.assetById(asset.getId()).getStatus(), "改访问级别不应动 status");

        assertEquals(0, assets.archiveAsset(-1L, at));
        assertEquals(0, assets.restoreAsset(-1L));
        assertEquals(0, assets.updateAccessLevel(-1L, "PUBLIC"));
    }

    /**
     * 资产分页筛选StillAgree之后Archiving处理入口。
     * 主要协作调用：<code>assets.insertAsset</code>、<code>assets.assetPageCount</code>、<code>assets.archiveAsset</code>、<code>assets.assetPage</code>。
     */
    @Test
    void assetPageFiltersStillAgreeAfterArchiving() {
        String keyword = unique("mapperxmlmediaarchive");
        MediaAssetEntity asset = newAsset(keyword + ".png", "IMAGE", "PUBLIC");
        assets.insertAsset(asset);

        assertEquals(1, assets.assetPageCount(keyword, null, "ACTIVE", null));
        assertEquals(1, assets.archiveAsset(asset.getId(), now()));
        assertEquals(0, assets.assetPageCount(keyword, null, "ACTIVE", null));
        assertEquals(1, assets.assetPageCount(keyword, null, "ARCHIVED", null));
        assertEquals(0, assets.assetPage(keyword, null, "ACTIVE", null, 0, 10).size(),
                "归档后的资产不应再出现在 ACTIVE 列表里");
    }

    /**
     * 按Exact身份读取referencesAppendList及删除。
     * 主要协作调用：<code>assets.insertAsset</code>、<code>references.countByMediaAssetId</code>、<code>references.selectByMediaAssetId</code>、<code>references.insertReference</code>、<code>references.deleteReference</code>。
     */
    @Test
    void referencesAppendListAndDeleteByExactIdentity() {
        MediaAssetEntity asset = newAsset(unique("mapperxmlmedia") + "-referenced.png", "IMAGE", "PROTECTED");
        assets.insertAsset(asset);
        long sourceId = probeId();

        assertEquals(0, references.countByMediaAssetId(asset.getId()));
        assertTrue(references.selectByMediaAssetId(asset.getId()).isEmpty());

        MediaReferenceEntity cover = newReference(asset.getId(), "BLOG", "POST", sourceId, "blog.cover");
        references.insertReference(cover);
        assertNotNull(cover.getId(), "insertReference 没有回填自增主键");
        assertEquals(1, references.countByMediaAssetId(asset.getId()));

        List<MediaReferenceEntity> rows = references.selectByMediaAssetId(asset.getId());
        assertEquals(1, rows.size());
        MediaReferenceEntity loaded = rows.get(0);
        assertEquals(asset.getId(), loaded.getMediaAssetId());
        assertEquals("BLOG", loaded.getSourceModule());
        assertEquals("POST", loaded.getSourceType());
        assertEquals(sourceId, loaded.getSourceId().longValue());
        assertEquals("blog.cover", loaded.getUsageCode());
        assertNotNull(loaded.getCreatedAt(), "created_at 由列默认值填充，应能读回");

        // 同一资产、同一来源的另一条用途：列表要按 created_at/id 升序返回两条
        MediaReferenceEntity content = newReference(asset.getId(), "BLOG", "POST", sourceId, "blog.content");
        references.insertReference(content);
        assertEquals(2, references.countByMediaAssetId(asset.getId()));
        List<MediaReferenceEntity> both = references.selectByMediaAssetId(asset.getId());
        assertEquals(2, both.size());
        assertEquals(cover.getId(), both.get(0).getId(), "引用列表必须按 created_at,id 升序");
        assertEquals(content.getId(), both.get(1).getId());

        assertEquals(1, references.deleteReference(asset.getId(), "BLOG", "POST", sourceId, "blog.cover"));
        assertEquals(0, references.deleteReference(asset.getId(), "BLOG", "POST", sourceId, "blog.cover"),
                "同一条引用不能被删两次");
        assertEquals(0, references.deleteReference(asset.getId(), "BLOG", "POST", sourceId + 1, "blog.content"),
                "source_id 不匹配时不能误删");
        assertEquals(1, references.countByMediaAssetId(asset.getId()));
        assertEquals(content.getId(), references.selectByMediaAssetId(asset.getId()).get(0).getId());

        assertEquals(0, references.countByMediaAssetId(probeId()), "别的资产不应被算进来");
    }

    /**
     * deleting全部References所属单次来源保留TheOtherSources处理入口。
     * 主要协作调用：<code>assets.insertAsset</code>、<code>references.insertReference</code>、<code>references.countByMediaAssetId</code>、<code>references.deleteAllBySource</code>、<code>references.selectByMediaAssetId</code>。
     */
    @Test
    void deletingAllReferencesOfOneSourceKeepsTheOtherSources() {
        MediaAssetEntity asset = newAsset(unique("mapperxmlmedia") + "-multi-source.png", "IMAGE", "PROTECTED");
        assets.insertAsset(asset);
        long postId = probeId();
        long chapterId = probeId();

        references.insertReference(newReference(asset.getId(), "BLOG", "POST", postId, "blog.cover"));
        references.insertReference(newReference(asset.getId(), "BLOG", "POST", postId, "blog.content"));
        references.insertReference(newReference(asset.getId(), "TUTORIAL", "CHAPTER", chapterId, "tutorial.body"));
        assertEquals(3, references.countByMediaAssetId(asset.getId()));

        assertEquals(2, references.deleteAllBySource("BLOG", "POST", postId),
                "按来源清理应当只删掉这一个来源的两条引用");
        assertEquals(1, references.countByMediaAssetId(asset.getId()),
                "其它模块的引用不能被顺手删掉");
        assertEquals("TUTORIAL", references.selectByMediaAssetId(asset.getId()).get(0).getSourceModule());
        assertEquals(0, references.deleteAllBySource("BLOG", "POST", postId), "已经没有可删的了");
    }

    /**
     * 回滚Leaves不含媒体测试样本行集合Behind。
     * 主要协作调用：<code>assets.insertAsset</code>、<code>references.insertReference</code>、<code>session.rollback</code>。
     */
    @Test
    void rollbackLeavesNoMediaProbeRowsBehind() {
        MediaAssetEntity asset = newAsset(unique("mapperxmlmedia-rollback") + ".png", "IMAGE", "PUBLIC");
        assets.insertAsset(asset);
        MediaReferenceEntity reference = newReference(asset.getId(), "BLOG", "POST", probeId(), "blog.cover");
        references.insertReference(reference);
        Long assetId = asset.getId();
        Long referenceId = reference.getId();
        assertNotNull(assetId);
        assertNotNull(referenceId);

        session.rollback();
        try (SqlSession fresh = openSession()) {
            assertNull(fresh.getMapper(MediaAssetMapper.class).assetById(assetId),
                    "回滚后不应在开发库里留下测试媒体资产");
            assertEquals(0, fresh.getMapper(MediaReferenceMapper.class).countByMediaAssetId(assetId),
                    "回滚后不应在开发库里留下测试媒体引用");
        }
    }

    /**
     * new资产处理入口。
     * 主要协作调用：<code>PROBE.incrementAndGet</code>。
     *
     * @param originalName 原始名称；类型为 <code>String</code>
     * @param mediaType 媒体类型；类型为 <code>String</code>
     * @param accessLevel 媒体访问级别；类型为 <code>String</code>
     * @return 类型为 <code>MediaAssetEntity</code> 的处理结果
     */
    private MediaAssetEntity newAsset(String originalName, String mediaType, String accessLevel) {
        String marker = unique("mapperxmlstorage");
        MediaAssetEntity asset = new MediaAssetEntity();
        asset.setOriginalName(originalName);
        asset.setMediaType(mediaType);
        asset.setMimeType("IMAGE".equals(mediaType) ? "image/png" : "application/pdf");
        asset.setFileExtension("IMAGE".equals(mediaType) ? "png" : "pdf");
        asset.setSizeBytes(2048L + PROBE.incrementAndGet());
        asset.setSha256(token64(marker));
        asset.setStorageProvider("LOCAL");
        asset.setStorageKey("mapper-xml-test/" + marker + ".bin");
        asset.setWidth("IMAGE".equals(mediaType) ? 1920 : null);
        asset.setHeight("IMAGE".equals(mediaType) ? 1080 : null);
        asset.setAccessLevel(accessLevel);
        asset.setStatus("ACTIVE");
        asset.setUploadedByAccountId(probeId());
        return asset;
    }

    /**
     * new引用处理入口。
     * 关键处理步骤调用 <code>reference.setMediaAssetId</code>、<code>reference.setSourceModule</code>、<code>reference.setSourceType</code>、<code>reference.setSourceId</code>。
     *
     * @param assetId 资产标识；类型为 <code>long</code>
     * @param module module；类型为 <code>String</code>
     * @param type 类型；类型为 <code>String</code>
     * @param sourceId 来源对象的业务标识；类型为 <code>long</code>
     * @param usageCode usage代码；类型为 <code>String</code>
     * @return 类型为 <code>MediaReferenceEntity</code> 的处理结果
     */
    private MediaReferenceEntity newReference(long assetId, String module, String type, long sourceId,
                                              String usageCode) {
        MediaReferenceEntity reference = new MediaReferenceEntity();
        reference.setMediaAssetId(assetId);
        reference.setSourceModule(module);
        reference.setSourceType(type);
        reference.setSourceId(sourceId);
        reference.setUsageCode(usageCode);
        return reference;
    }

    /* storage_provider + storage_key、sha256 都是 char(64)/唯一键，拿足位数避免被空格补齐影响比较 */
    /**
     * 令牌64处理入口。
     * 关键处理步骤调用 <code>seed.replaceAll</code>、<code>builder.length</code>、<code>builder.append</code>、<code>builder.substring</code>。
     *
     * @param seed seed；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private String token64(String seed) {
        String cleaned = seed.replaceAll("[^A-Za-z0-9]", "");
        StringBuilder builder = new StringBuilder();
        while (builder.length() < 64) {
            builder.append(cleaned);
        }
        return builder.substring(0, 64);
    }

    /* 逻辑外键，库里没有物理 FOREIGN KEY：用一个大号段避免和真实账户撞号 */
    /**
     * 测试样本标识处理入口。
     * 主要协作调用：<code>PROBE.incrementAndGet</code>。
     * @return 类型为 <code>long</code> 的处理结果
     */
    private long probeId() {
        return 900_000_000_000L + PROBE.incrementAndGet();
    }

    /**
     * unique处理入口。
     * 主要协作调用：<code>PROBE.incrementAndGet</code>。
     *
     * @param prefix prefix；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private String unique(String prefix) {
        return prefix + "-" + PROBE.incrementAndGet() + "-" + System.nanoTime();
    }

    /**
     * now处理入口。
     * 关键处理步骤调用 <code>LocalDateTime.now</code>。
     * @return 类型为 <code>LocalDateTime</code> 的处理结果
     */
    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    }
}
