/*
 * BlogContentMediaParser：博客模块的工具文件。
 * 业务范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 * 阅读入口：extractMediaAssetIds（提取媒体资产标识集合）。
 */

package com.starrainnotes.blog.utils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * 从正文 Markdown 里解析 blog.content 媒体引用。
 *
 * 为什么不让前端另外提交一份「正文用到的媒体 id 列表」：
 * 正文才是唯一真源。两份数据一旦不同步，就会出现“库里说引用了、正文里没有”
 * 或者反过来的情况，而 Media 侧的归档保护只会依据引用表判断，误判代价很高。
 *
 * 只识别 Media 模块公开的内容地址（/api/media/assets/{id}/content），
 * 不解析任意 URL：外部图床地址不构成对本站媒体的引用。
 */
/**
 * 博客内容媒体Parser，属于博客模块。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 */
public final class BlogContentMediaParser {

    /*
     * 匹配本站媒体的公开内容地址。
     *
     * 前置断言 (?<![A-Za-z0-9.\-]) 是必要的：没有它，
     * https://cdn.example.com/api/media/assets/9/content.png 这种外部地址
     * 也会被当成“引用了本站 9 号媒体”，于是 Media 会永远拒绝归档它。
     */
    /**
     * 地址。
     */
    private static final Pattern CONTENT_URL =
            Pattern.compile("(?<![A-Za-z0-9.\\-])/api/media/assets/(\\d+)/content");

    /**
     * 构造BlogContentMediaParser，接收本类使用的依赖或初始化参数。
     */
    private BlogContentMediaParser() {
    }

    // 保持出现顺序并去重：引用顺序稳定，便于测试与排障
    /**
     * 提取媒体资产标识集合。
     * 主要协作调用：<code>CONTENT_URL.matcher</code>。
     * 边界判断包含 <code>(bodyMarkdown == null || bodyMarkdown.isBlank())</code>；<code>(id &gt; 0)</code>。
     *
     * @param bodyMarkdown Markdown 格式的正文；类型为 <code>String</code>
     * @return 本次处理得到的条目集合
     */
    public static List<Long> extractMediaAssetIds(String bodyMarkdown) {
        if (bodyMarkdown == null || bodyMarkdown.isBlank()) {
            return List.of();
        }
        Set<Long> ids = new LinkedHashSet<>();
        Matcher matcher = CONTENT_URL.matcher(bodyMarkdown);
        while (matcher.find()) {
            try {
                long id = Long.parseLong(matcher.group(1));
                if (id > 0) {
                    ids.add(id);
                }
            } catch (NumberFormatException ex) {
                // 形如 /api/media/assets/999999999999999999999/content：超出 long 范围，
                // 属于正文里的无效链接，忽略而不是让整篇保存失败
            }
        }
        return new ArrayList<>(ids);
    }
}
