/*
 * SeoMetaFormatter：公开页面与 SEO模块的工具文件。
 * 业务范围：公开 HTML 快照、规范地址、站点地图和搜索引擎通知。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 * 主要协作类型：SeoSourceDocument。
 * 阅读入口：title（标题处理入口）；description（描述处理入口）。
 */

package com.starrainnotes.seo.utils;

import com.starrainnotes.seo.dto.SeoSourceDocument;

/**
 * SEOMetaFormatter，属于公开页面与 SEO模块。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 */
public final class SeoMetaFormatter {
    /**
     * 构造SeoMetaFormatter，接收本类使用的依赖或初始化参数。
     */
    private SeoMetaFormatter() {}

    /**
     * 标题处理入口。
     * 关键处理步骤调用 <code>source.getTitle</code>、<code>title.isEmpty</code>、<code>title.equals</code>、<code>truncate</code>。
     * 边界判断包含 <code>(title.isEmpty())</code>。
     *
     * @param source 来源；类型为 <code>SeoSourceDocument</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    public static String title(SeoSourceDocument source) {
        String title = source.getTitle() == null ? "" : source.getTitle().strip();
        if (title.isEmpty()) title = "星雨笔录";
        return title.equals("星雨笔录") ? title : truncate(title, 220) + " | 星雨笔录";
    }

    /**
     * 描述处理入口。
     * 关键处理步骤调用 <code>source.getSummary</code>、<code>summary.isEmpty</code>、<code>truncate</code>、<code>summary.replaceAll</code>。
     * 边界判断包含 <code>(summary.isEmpty())</code>。
     *
     * @param source 来源；类型为 <code>SeoSourceDocument</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    public static String description(SeoSourceDocument source) {
        String summary = source.getSummary() == null ? "" : source.getSummary().strip();
        if (summary.isEmpty()) summary = "星雨笔录：知识、代码与成长。";
        return truncate(summary.replaceAll("\\s+", " "), 300);
    }

    /**
     * truncate处理入口。
     * 关键处理步骤调用 <code>text.length</code>、<code>text.substring</code>。
     *
     * @param text 文本；类型为 <code>String</code>
     * @param max 最大；类型为 <code>int</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    private static String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max);
    }
}
