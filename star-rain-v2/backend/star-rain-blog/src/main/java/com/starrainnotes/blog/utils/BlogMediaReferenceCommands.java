package com.starrainnotes.blog.utils;

import com.starrainnotes.blog.constant.BlogMediaReference;
import com.starrainnotes.media.api.dto.MediaReferenceCommand;
import com.starrainnotes.media.constant.MediaUsageCodes;

/*
 * 构造 Blog 的媒体引用命令。
 *
 * 集中在这里是为了保证 sourceModule / sourceType / usageCode 三者永远配套：
 * Media 会校验 usageCode 前缀必须等于 sourceModule 小写形式，
 * 散落的字面量迟早会出现 blog.cover 被挂到 TUTORIAL 上这类错误。
 */
public final class BlogMediaReferenceCommands {

    private BlogMediaReferenceCommands() {
    }

    // 封面引用：一篇文章最多一个 blog.cover
    public static MediaReferenceCommand cover(Long mediaAssetId, Long postId) {
        return command(mediaAssetId, postId, MediaUsageCodes.BLOG_COVER);
    }

    // 正文内嵌媒体引用：正文里出现多少个内容地址，就有多少条 blog.content
    public static MediaReferenceCommand content(Long mediaAssetId, Long postId) {
        return command(mediaAssetId, postId, MediaUsageCodes.BLOG_CONTENT);
    }

    private static MediaReferenceCommand command(Long mediaAssetId, Long postId, String usageCode) {
        return MediaReferenceCommand.builder()
                .mediaAssetId(mediaAssetId)
                .sourceModule(BlogMediaReference.SOURCE_MODULE)
                .sourceType(BlogMediaReference.SOURCE_TYPE_POST)
                .sourceId(postId)
                .usageCode(usageCode)
                .build();
    }
}
