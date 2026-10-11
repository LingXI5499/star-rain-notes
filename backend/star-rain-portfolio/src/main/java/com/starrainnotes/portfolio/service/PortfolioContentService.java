/*
 * PortfolioContentService：作品模块的业务契约文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：WorkSectionDTO。
 * 阅读入口：taxonomy（taxonomy处理入口）；templates（templates处理入口）；sections（sections处理入口）；tags（标签列表处理入口）。
 */

package com.starrainnotes.portfolio.service;
import com.starrainnotes.portfolio.dto.WorkSectionDTO;
import com.starrainnotes.portfolio.vo.*;
import java.util.List;
import java.util.Map;
/**
 * 作品内容业务服务，属于作品模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface PortfolioContentService {
    /**
     * taxonomy处理入口。
     * @return 类型为 <code>Map&lt;String, List&lt;WorkTaxonomyVO&gt;&gt;</code> 的处理结果
     */
    Map<String, List<WorkTaxonomyVO>> taxonomy();
    /**
     * templates处理入口。
     * @return 本次处理得到的条目集合
     */
    List<WorkTemplateVO> templates();
    /**
     * sections处理入口。
     *
     * @param workId 作品标识；类型为 <code>Long</code>
     * @param publicOnly 公开Only；类型为 <code>boolean</code>
     * @return 本次处理得到的条目集合
     */
    List<WorkSectionVO> sections(Long workId, boolean publicOnly);
    /**
     * 标签列表处理入口。
     *
     * @param workId 作品标识；类型为 <code>Long</code>
     * @return 本次处理得到的条目集合
     */
    List<WorkTaxonomyVO> tags(Long workId);
    /**
     * 设置标签列表。
     *
     * @param workId 作品标识；类型为 <code>Long</code>
     * @param ids 标识集合；类型为 <code>List&lt;Long&gt;</code>
     */
    void setTags(Long workId, List<Long> ids);
    /**
     * copyTemplate处理入口。
     *
     * @param workId 作品标识；类型为 <code>Long</code>
     * @param templateId template标识；类型为 <code>Long</code>
     */
    void copyTemplate(Long workId, Long templateId);
    /**
     * 创建作品内容。
     *
     * @param workId 作品标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>WorkSectionDTO</code>
     * @return 类型为 <code>WorkSectionVO</code> 的处理结果
     */
    WorkSectionVO create(Long workId, WorkSectionDTO request);
    /**
     * 更新作品内容。
     *
     * @param workId 作品标识；类型为 <code>Long</code>
     * @param sectionId 区块标识；类型为 <code>Long</code>
     * @param request 本次操作的请求参数对象；类型为 <code>WorkSectionDTO</code>
     * @return 类型为 <code>WorkSectionVO</code> 的处理结果
     */
    WorkSectionVO update(Long workId, Long sectionId, WorkSectionDTO request);
    /**
     * 删除作品内容。
     *
     * @param workId 作品标识；类型为 <code>Long</code>
     * @param sectionId 区块标识；类型为 <code>Long</code>
     */
    void delete(Long workId, Long sectionId);
    /**
     * 调整顺序作品内容。
     *
     * @param workId 作品标识；类型为 <code>Long</code>
     * @param ids 标识集合；类型为 <code>List&lt;Long&gt;</code>
     */
    void reorder(Long workId, List<Long> ids);
    /**
     * 删除全部。
     *
     * @param workId 作品标识；类型为 <code>Long</code>
     */
    void deleteAll(Long workId);
    /**
     * 校验发布。
     *
     * @param workId 作品标识；类型为 <code>Long</code>
     */
    void validatePublication(Long workId);
    /**
     * 可搜索文本处理入口。
     *
     * @param workId 作品标识；类型为 <code>Long</code>
     * @param legacy legacy；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    String searchableText(Long workId, String legacy);
    /**
     * SEOMarkdown处理入口。
     *
     * @param workId 作品标识；类型为 <code>Long</code>
     * @param legacy legacy；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    String seoMarkdown(Long workId, String legacy);
    /**
     * 校验Taxonomy。
     *
     * @param categoryId 分类标识；类型为 <code>Long</code>
     * @param formatId 格式标识；类型为 <code>Long</code>
     */
    void validateTaxonomy(Long categoryId, Long formatId);
    /**
     * 校验精选。
     *
     * @param workId 作品标识；类型为 <code>Long</code>
     * @param featured 精选；类型为 <code>boolean</code>
     */
    void validateFeatured(Long workId, boolean featured);
}
