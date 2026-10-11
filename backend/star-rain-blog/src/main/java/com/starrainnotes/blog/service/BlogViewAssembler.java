/*
 * BlogViewAssembler：博客模块的业务契约文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 * 主要协作类型：BlogPostDocument、BlogPostSummary、BlogPostEntity、BlogTagEntity、BlogPostAdminDetailVO、BlogPostAdminVO、BlogPostPublicDetailVO。
 * 阅读入口：toTagVO（转换为标签响应对象）；tagsOf（标签列表所属处理入口）；topicsOf（专题列表所属处理入口）；coverUrl（封面地址处理入口）。
 */

package com.starrainnotes.blog.service;

import com.starrainnotes.blog.api.dto.BlogPostDocument;
import com.starrainnotes.blog.api.dto.BlogPostSummary;
import com.starrainnotes.blog.entity.BlogPostEntity;
import com.starrainnotes.blog.entity.BlogTagEntity;
import com.starrainnotes.blog.vo.BlogPostAdminDetailVO;
import com.starrainnotes.blog.vo.BlogPostAdminVO;
import com.starrainnotes.blog.vo.BlogPostPublicDetailVO;
import com.starrainnotes.blog.vo.BlogPostPublicVO;
import com.starrainnotes.blog.vo.BlogTagVO;
import com.starrainnotes.blog.vo.BlogTopicVO;
import java.util.List;

/*
 * 文章视图组装契约。
 *
 * 单独抽出来是因为同一篇文章要按三种口径输出（后台列表 / 后台详情 / 前台），
 * 而分类与封面地址的取法完全一致。如果每个 Service 各写一份映射，
 * 迟早会出现“后台显示了封面、前台没有”这类只有肉眼能发现的偏差。
 */
/**
 * 博客View组装器，属于博客模块。
 * 声明业务服务的输入和输出，使接入层与具体实现分离；状态转换和数据写入由实现类处理。
 */
public interface BlogViewAssembler {

    /**
     * 转换为标签响应对象。
     *
     * @param entity 数据库映射实体；类型为 <code>BlogTagEntity</code>
     * @return 类型为 <code>BlogTagVO</code> 的处理结果
     */
    BlogTagVO toTagVO(BlogTagEntity entity);

    /**
     * 标签列表所属处理入口。
     *
     * @param postId 文章标识；类型为 <code>Long</code>
     * @return 本次处理得到的条目集合
     */
    List<BlogTagVO> tagsOf(Long postId);

    /**
     * 专题列表所属处理入口。
     *
     * @param postId 文章标识；类型为 <code>Long</code>
     * @return 本次处理得到的条目集合
     */
    List<BlogTopicVO> topicsOf(Long postId);

    // 封面地址由 Media 模块给出；封面不存在或已归档时返回 null，而不是拼一个会 404 的地址
    /**
     * 封面地址处理入口。
     *
     * @param mediaAssetId 关联的媒体资产标识；类型为 <code>Long</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    String coverUrl(Long mediaAssetId);

    /**
     * 转换为后台响应对象。
     *
     * @param post 文章；类型为 <code>BlogPostEntity</code>
     * @return 类型为 <code>BlogPostAdminVO</code> 的处理结果
     */
    BlogPostAdminVO toAdminVO(BlogPostEntity post);

    // 列表用：分类一次性批量取回，避免每篇文章各查一次
    /**
     * 转换为后台VOs。
     *
     * @param posts 文章列表；类型为 <code>List&lt;BlogPostEntity&gt;</code>
     * @return 本次处理得到的条目集合
     */
    List<BlogPostAdminVO> toAdminVOs(List<BlogPostEntity> posts);

    /**
     * 转换为后台详情响应对象。
     *
     * @param post 文章；类型为 <code>BlogPostEntity</code>
     * @return 类型为 <code>BlogPostAdminDetailVO</code> 的处理结果
     */
    BlogPostAdminDetailVO toAdminDetailVO(BlogPostEntity post);

    /**
     * 转换为公开响应对象。
     *
     * @param post 文章；类型为 <code>BlogPostEntity</code>
     * @return 类型为 <code>BlogPostPublicVO</code> 的处理结果
     */
    BlogPostPublicVO toPublicVO(BlogPostEntity post);

    /**
     * 转换为公开VOs。
     *
     * @param posts 文章列表；类型为 <code>List&lt;BlogPostEntity&gt;</code>
     * @return 本次处理得到的条目集合
     */
    List<BlogPostPublicVO> toPublicVOs(List<BlogPostEntity> posts);

    /**
     * 转换为公开详情响应对象。
     *
     * @param post 文章；类型为 <code>BlogPostEntity</code>
     * @return 类型为 <code>BlogPostPublicDetailVO</code> 的处理结果
     */
    BlogPostPublicDetailVO toPublicDetailVO(BlogPostEntity post);

    /**
     * 转换为摘要。
     *
     * @param post 文章；类型为 <code>BlogPostEntity</code>
     * @return 类型为 <code>BlogPostSummary</code> 的处理结果
     */
    BlogPostSummary toSummary(BlogPostEntity post);

    /**
     * 转换为文档。
     *
     * @param post 文章；类型为 <code>BlogPostEntity</code>
     * @return 类型为 <code>BlogPostDocument</code> 的处理结果
     */
    BlogPostDocument toDocument(BlogPostEntity post);
}
