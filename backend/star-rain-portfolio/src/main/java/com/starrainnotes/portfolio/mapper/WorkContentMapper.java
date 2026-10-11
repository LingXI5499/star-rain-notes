/*
 * WorkContentMapper：作品模块的数据访问文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 * 阅读入口：categories（categories处理入口）；formats（formats处理入口）；tags（标签列表处理入口）；workTags（作品标签列表处理入口）。
 */

package com.starrainnotes.portfolio.mapper;
import com.starrainnotes.portfolio.entity.*;
import java.util.List;
import org.apache.ibatis.annotations.Param;
/**
 * 作品内容数据映射，属于作品模块。
 * 定义持久化查询与写入操作。参数名与 MyBatis 映射保持一致，业务状态判断由调用服务与 SQL 条件共同完成。
 */
public interface WorkContentMapper {
 /**
  * categories处理入口。
  *
  * 持久化操作：读取，涉及 <code>sr_portfolio_category</code>。
  * @return 本次处理得到的条目集合
  */
 List<WorkTaxonomyEntity> categories();
 /**
  * formats处理入口。
  *
  * 持久化操作：读取，涉及 <code>sr_portfolio_format</code>。
  * @return 本次处理得到的条目集合
  */
 List<WorkTaxonomyEntity> formats();
 /**
  * 标签列表处理入口。
  *
  * 持久化操作：读取，涉及 <code>sr_portfolio_tag</code>。
  * @return 本次处理得到的条目集合
  */
 List<WorkTaxonomyEntity> tags();
 /**
  * 作品标签列表处理入口。
  *
  * 持久化操作：读取，涉及 <code>sr_portfolio_tag</code>、<code>sr_portfolio_work_tag</code>。
  *
  * @param workId 作品标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("workId")</code>
  * @return 本次处理得到的条目集合
  */
 List<WorkTaxonomyEntity> workTags(@Param("workId") Long workId);
 /**
  * templates处理入口。
  *
  * 持久化操作：读取，涉及 <code>sr_portfolio_template</code>。
  * @return 本次处理得到的条目集合
  */
 List<WorkTemplateEntity> templates();
 /**
  * template处理入口。
  *
  * 持久化操作：读取，涉及 <code>sr_portfolio_template</code>。
  *
  * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
  * @return 类型为 <code>WorkTemplateEntity</code> 的处理结果
  */
 WorkTemplateEntity template(@Param("id") Long id);
 /**
  * 锁精选处理入口。
  *
  * 持久化操作：读取，涉及 <code>sr_portfolio_category</code>。
  * 此查询使用行锁；调用方应在事务中完成后续状态检查和写入。
  * @return 类型为 <code>Long</code> 的处理结果
  */
 Long lockFeatured();
 /**
  * 精选数量处理入口。
  *
  * 持久化操作：读取，涉及 <code>sr_portfolio_work</code>。
  *
  * @param excludeId exclude标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("excludeId")</code>
  * @return 符合 SQL 条件的记录数量
  */
 long featuredCount(@Param("excludeId") Long excludeId);
 /**
  * 清理标签列表。
  *
  * 持久化操作：删除，涉及 <code>sr_portfolio_work_tag</code>。
  *
  * @param workId 作品标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("workId")</code>
  */
 void clearTags(@Param("workId") Long workId);
 /**
  * 添加标签。
  *
  * 持久化操作：新增，涉及 <code>sr_portfolio_work_tag</code>。
  *
  * @param workId 作品标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("workId")</code>
  * @param tagId 标签标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("tagId")</code>
  */
 void addTag(@Param("workId") Long workId,@Param("tagId") Long tagId);
 /**
  * sections处理入口。
  *
  * 持久化操作：读取，涉及 <code>sr_portfolio_section</code>。
  *
  * @param workId 作品标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("workId")</code>
  * @return 本次处理得到的条目集合
  */
 List<WorkSectionEntity> sections(@Param("workId") Long workId);
 /**
  * 区块处理入口。
  *
  * 持久化操作：读取，涉及 <code>sr_portfolio_section</code>。
  *
  * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
  * @return 类型为 <code>WorkSectionEntity</code> 的处理结果
  */
 WorkSectionEntity section(@Param("id") Long id);
 /**
  * 写入区块。
  *
  * 持久化操作：新增，涉及 <code>sr_portfolio_section</code>。
  *
  * @param row 行；类型为 <code>WorkSectionEntity</code>
  */
 void insertSection(WorkSectionEntity row);
 /**
  * 更新区块。
  *
  * 持久化操作：更新，涉及 <code>sr_portfolio_section</code>。
  *
  * @param row 行；类型为 <code>WorkSectionEntity</code>
  */
 void updateSection(WorkSectionEntity row);
 /**
  * 顺序区块处理入口。
  *
  * 持久化操作：更新，涉及 <code>sr_portfolio_section</code>。
  *
  * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
  * @param sortOrder 展示排序值；类型为 <code>int</code>；对应 MyBatis 命名参数 <code>@Param("sortOrder")</code>
  */
 void orderSection(@Param("id") Long id,@Param("sortOrder") int sortOrder);
 /**
  * 删除区块。
  *
  * 持久化操作：删除，涉及 <code>sr_portfolio_section</code>。
  *
  * @param id 当前业务对象的数据库标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("id")</code>
  */
 void deleteSection(@Param("id") Long id);
 /**
  * 区块媒体处理入口。
  *
  * 持久化操作：读取，涉及 <code>sr_portfolio_section_media</code>。
  *
  * @param sectionId 区块标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("sectionId")</code>
  * @return 本次处理得到的条目集合
  */
 List<WorkSectionMediaEntity> sectionMedia(@Param("sectionId") Long sectionId);
 /**
  * 写入区块媒体。
  *
  * 持久化操作：新增，涉及 <code>sr_portfolio_section_media</code>。
  *
  * @param row 行；类型为 <code>WorkSectionMediaEntity</code>
  */
 void insertSectionMedia(WorkSectionMediaEntity row);
 /**
  * 清理区块媒体。
  *
  * 持久化操作：删除，涉及 <code>sr_portfolio_section_media</code>。
  *
  * @param sectionId 区块标识；类型为 <code>Long</code>；对应 MyBatis 命名参数 <code>@Param("sectionId")</code>
  */
 void clearSectionMedia(@Param("sectionId") Long sectionId);
}
