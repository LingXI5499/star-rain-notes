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
public interface BlogViewAssembler {

    BlogTagVO toTagVO(BlogTagEntity entity);

    List<BlogTagVO> tagsOf(Long postId);

    List<BlogTopicVO> topicsOf(Long postId);

    // 封面地址由 Media 模块给出；封面不存在或已归档时返回 null，而不是拼一个会 404 的地址
    String coverUrl(Long mediaAssetId);

    BlogPostAdminVO toAdminVO(BlogPostEntity post);

    // 列表用：分类一次性批量取回，避免每篇文章各查一次
    List<BlogPostAdminVO> toAdminVOs(List<BlogPostEntity> posts);

    BlogPostAdminDetailVO toAdminDetailVO(BlogPostEntity post);

    BlogPostPublicVO toPublicVO(BlogPostEntity post);

    List<BlogPostPublicVO> toPublicVOs(List<BlogPostEntity> posts);

    BlogPostPublicDetailVO toPublicDetailVO(BlogPostEntity post);

    BlogPostSummary toSummary(BlogPostEntity post);

    BlogPostDocument toDocument(BlogPostEntity post);
}
