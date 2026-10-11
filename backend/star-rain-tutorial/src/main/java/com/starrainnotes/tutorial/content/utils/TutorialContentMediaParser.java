/*
 * TutorialContentMediaParser：教程与学习模块的工具文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 * 阅读入口：extractMediaAssetIds（提取媒体资产标识集合）。
 */

package com.starrainnotes.tutorial.content.utils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 仅登记本站媒体地址；外部图床不属于本站媒体引用。
/**
 * 教程内容媒体Parser，属于教程与学习模块。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 */
public final class TutorialContentMediaParser {
    /**
     * 地址。
     */
    private static final Pattern CONTENT_URL =
            Pattern.compile("(?<![A-Za-z0-9.\\-])/api/media/assets/(\\d+)/content");

    /**
     * 构造TutorialContentMediaParser，接收本类使用的依赖或初始化参数。
     */
    private TutorialContentMediaParser() {
    }

    /**
     * 提取媒体资产标识集合。
     * 主要协作调用：<code>CONTENT_URL.matcher</code>。
     * 边界判断包含 <code>(markdown == null || markdown.isBlank())</code>；<code>(id &gt; 0)</code>。
     *
     * @param markdown Markdown；类型为 <code>String</code>
     * @return 本次处理得到的条目集合
     */
    public static List<Long> extractMediaAssetIds(String markdown) {
        if (markdown == null || markdown.isBlank()) return List.of();
        Set<Long> ids = new LinkedHashSet<>();
        Matcher matcher = CONTENT_URL.matcher(markdown);
        while (matcher.find()) {
            try {
                long id = Long.parseLong(matcher.group(1));
                if (id > 0) ids.add(id);
            } catch (NumberFormatException ignored) {
                // 正文里的超长无效地址不应妨碍保存章节。
            }
        }
        return new ArrayList<>(ids);
    }
}
