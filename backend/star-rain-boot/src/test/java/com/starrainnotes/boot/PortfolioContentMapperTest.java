/*
 * PortfolioContentMapperTest：应用启动模块的测试文件。
 * 业务范围：多模块装配、入口路由、安全配置和启动初始化。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：WorkFilterDTO。
 * 阅读入口：taxonomyTemplatesSectionsReferencesAndFiltersRoundTripInOneRollback（taxonomyTemplatesSectionsReferences及筛选RoundTrip内部单次回滚处理入口）。
 */

package com.starrainnotes.boot;
import static org.junit.jupiter.api.Assertions.*;
import com.starrainnotes.portfolio.dto.WorkFilterDTO;
import com.starrainnotes.portfolio.entity.*;
import com.starrainnotes.portfolio.mapper.*;
import org.junit.jupiter.api.Test;
/**
 * 作品内容数据映射测试，属于应用启动模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class PortfolioContentMapperTest extends MapperXmlIntegrationSupport {
 /**
  * taxonomyTemplatesSectionsReferences及筛选RoundTrip内部单次回滚处理入口。
  * 关键处理步骤调用 <code>openSession</code>、<code>session.getMapper</code>、<code>blocks.categories</code>、<code>blocks.formats</code>。
  */
 @Test void taxonomyTemplatesSectionsReferencesAndFiltersRoundTripInOneRollback() {
  try (var session = openSession()) {
   var works = session.getMapper(WorkMapper.class); var blocks = session.getMapper(WorkContentMapper.class);
   assertEquals(5, blocks.categories().size()); assertEquals(20,blocks.formats().size()); assertEquals(6,blocks.templates().size());
   var work = new WorkEntity(); work.setSlug("works-sql-"+System.nanoTime()); work.setTitle("Works SQL fixture"); work.setWorkType("SOFTWARE"); work.setBodyMarkdown("legacy"); work.setSummary("summary"); work.setStatus("DRAFT"); work.setCreatedByAccountId(1L); work.setUpdatedByAccountId(1L);
   work.setCategoryId(blocks.categories().getFirst().getId()); work.setFormatId(blocks.formats().getFirst().getId());
   works.insert(work); assertNotNull(work.getId());
   var first = WorkSectionEntity.builder().workId(work.getId()).sectionType("MARKDOWN").title("SQL block").content("visible words").dataJson("{}").blockVersion(1).visible(true).sortOrder(10).build();
   var second = WorkSectionEntity.builder().workId(work.getId()).sectionType("QUOTE").title("Hidden").content("private words").dataJson("{\"author\":\"author\"}").blockVersion(1).visible(false).sortOrder(20).build();
   blocks.insertSection(first); blocks.insertSection(second); assertEquals(2,blocks.sections(work.getId()).size()); assertFalse(blocks.section(second.getId()).getVisible());
   blocks.orderSection(second.getId(),0); assertEquals(second.getId(),blocks.sections(work.getId()).getFirst().getId());
   second.setTitle("updated title"); blocks.updateSection(second); assertEquals("updated title",blocks.section(second.getId()).getTitle());
   var media = WorkSectionMediaEntity.builder().sectionId(first.getId()).mediaAssetId(99999L).caption("caption").sortOrder(10).build();
   blocks.insertSectionMedia(media); assertNotNull(media.getId()); assertEquals(99999L,blocks.sectionMedia(first.getId()).getFirst().getMediaAssetId());
   var tag = blocks.tags().getFirst(); blocks.addTag(work.getId(),tag.getId()); assertEquals(tag.getId().toString(),blocks.workTags(work.getId()).getFirst().getId().toString());
   var filter = WorkFilterDTO.builder().categoryId(work.getCategoryId()).formatId(work.getFormatId()).tagId(tag.getId()).featured(false).build();
   assertEquals(1,works.countByTaxonomy(null,"DRAFT",work.getTitle(),filter)); assertEquals(work.getId(),works.pageByTaxonomy(null,"DRAFT",work.getTitle(),filter,10,0).getFirst().getId());
   assertEquals(0,works.countByTaxonomy(null,"PUBLISHED",work.getTitle(),filter));
   blocks.clearSectionMedia(first.getId()); blocks.deleteSection(first.getId()); assertNull(blocks.section(first.getId()));
   blocks.clearTags(work.getId()); assertTrue(blocks.workTags(work.getId()).isEmpty());
   session.rollback();
  }
 }
}
