/*
 * BlogMediaReferenceCommands：博客模块的工具文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 * 主要协作类型：BlogMediaReference、MediaReferenceCommand、MediaUsageCodes。
 * 阅读入口：cover（封面处理入口）；content（内容处理入口）。
 */

package com.starrainnotes.blog.utils;

import com.starrainnotes.blog.constant.BlogMediaReference;
import com.starrainnotes.media.api.dto.MediaReferenceCommand;
import com.starrainnotes.media.api.constant.MediaUsageCodes;

/*
 * 构造 Blog 的媒体引用命令。
 *
 * 集中在这里是为了保证 sourceModule / sourceType / usageCode 三者永远配套：
 * Media 会校验 usageCode 前缀必须等于 sourceModule 小写形式，
 * 散落的字面量迟早会出现 blog.cover 被挂到 TUTORIAL 上这类错误。
 */
/**
 * 博客媒体引用Commands，属于博客模块。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 */
public final class BlogMediaReferenceCommands {

    /**
     * 构造BlogMediaReferenceCommands，接收本类使用的依赖或初始化参数。
     */
    private BlogMediaReferenceCommands() {
    }

    // 封面引用：一篇文章最多一个 blog.cover
    /**
     * 封面处理入口。
     * 关键处理步骤调用 <code>command</code>。
     *
     * @param mediaAssetId 关联的媒体资产标识；类型为 <code>Long</code>
     * @param postId 文章标识；类型为 <code>Long</code>
     * @return 类型为 <code>MediaReferenceCommand</code> 的处理结果
     */
    public static MediaReferenceCommand cover(Long mediaAssetId, Long postId) {
        return command(mediaAssetId, postId, MediaUsageCodes.BLOG_COVER);
    }

    // 正文内嵌媒体引用：正文里出现多少个内容地址，就有多少条 blog.content
    /**
     * 内容处理入口。
     * 关键处理步骤调用 <code>command</code>。
     *
     * @param mediaAssetId 关联的媒体资产标识；类型为 <code>Long</code>
     * @param postId 文章标识；类型为 <code>Long</code>
     * @return 类型为 <code>MediaReferenceCommand</code> 的处理结果
     */
    public static MediaReferenceCommand content(Long mediaAssetId, Long postId) {
        return command(mediaAssetId, postId, MediaUsageCodes.BLOG_CONTENT);
    }

    /**
     * command处理入口。
     * 关键处理步骤调用 <code>MediaReferenceCommand.builder</code>。
     *
     * @param mediaAssetId 关联的媒体资产标识；类型为 <code>Long</code>
     * @param postId 文章标识；类型为 <code>Long</code>
     * @param usageCode usage代码；类型为 <code>String</code>
     * @return 类型为 <code>MediaReferenceCommand</code> 的处理结果
     */
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
