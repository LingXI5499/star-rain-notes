package com.starrainnotes.blog.assembler;

import com.starrainnotes.blog.entity.BlogPost;
import com.starrainnotes.blog.entity.BlogTag;
import com.starrainnotes.blog.vo.BlogPostAdminDetailVO;
import com.starrainnotes.blog.vo.BlogPostAdminSummaryVO;
import com.starrainnotes.blog.vo.BlogPostNeighborVO;
import com.starrainnotes.blog.vo.BlogPostPublicDetailVO;
import com.starrainnotes.blog.vo.BlogPostPublicSummaryVO;
import com.starrainnotes.blog.vo.BlogTagVO;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Explicit, reusable mapping from persistence rows to the frozen API schema. */
@Component
@RequiredArgsConstructor
public class BlogPostAssembler {
    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private final SiteSettingsTimezone timezone;

    public BlogPostAdminDetailVO toAdminDetail(BlogPost post, List<BlogTag> tags, String coverUrl) {
        return new BlogPostAdminDetailVO(post.getId(), post.getTitle(), post.getSlug(), post.getSummary(),
                post.getBodyMarkdown(), post.getCoverMediaId(), coverUrl, post.getPublishStatus(), post.getSeoTitle(),
                post.getSeoDescription(), format(post.getPublishedAt()), format(post.getCreatedAt()), format(post.getUpdatedAt()),
                toTagVOs(tags));
    }

    public BlogPostAdminSummaryVO toAdminSummary(BlogPost post, List<BlogTagVO> tags, String coverUrl) {
        return new BlogPostAdminSummaryVO(post.getId(), post.getTitle(), post.getSlug(), post.getSummary(), coverUrl,
                post.getPublishStatus(), format(post.getPublishedAt()), format(post.getUpdatedAt()), tags);
    }

    public BlogPostPublicSummaryVO toPublicSummary(BlogPost post, List<BlogTagVO> tags, String coverUrl) {
        return new BlogPostPublicSummaryVO(post.getId(), post.getTitle(), post.getSlug(), post.getSummary(), coverUrl,
                format(post.getPublishedAt()), format(post.getUpdatedAt()), tags);
    }

    public BlogPostPublicDetailVO toPublicDetail(BlogPost post, List<BlogTag> tags, String coverUrl,
                                                  BlogPostNeighborVO previous, BlogPostNeighborVO next) {
        return new BlogPostPublicDetailVO(post.getId(), post.getTitle(), post.getSlug(), post.getSummary(),
                post.getBodyMarkdown(), coverUrl, toTagVOs(tags), post.getSeoTitle(), post.getSeoDescription(),
                format(post.getPublishedAt()), format(post.getUpdatedAt()), previous, next);
    }

    public BlogTagVO toTagVO(BlogTag tag) {
        return new BlogTagVO(tag.getId(), tag.getName(), tag.getSlug());
    }

    public List<BlogTagVO> toTagVOs(List<BlogTag> tags) {
        return tags.stream().map(this::toTagVO).toList();
    }

    public BlogPostNeighborVO toNeighborVO(BlogPost post) {
        return post == null ? null : new BlogPostNeighborVO(post.getId(), post.getSlug(), post.getTitle());
    }

    private String format(LocalDateTime utc) {
        return utc == null ? null : timezone.atSite(utc).format(ISO_OFFSET);
    }
}
