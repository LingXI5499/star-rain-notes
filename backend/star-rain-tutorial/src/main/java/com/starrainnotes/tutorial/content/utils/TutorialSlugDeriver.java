/*
 * TutorialSlugDeriver：教程与学习模块的工具文件。
 * 业务范围：课程目录、章节版本、审核工作区、阅读进度、知识卡片和答题记录。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 * 阅读入口：fromTitle（从输入构造标题）。
 */

package com.starrainnotes.tutorial.content.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.Locale;

/*
 * 教程、知识体系与章节的地址编号由服务端生成，作者只填写可读名称。
 * 对纯中文名称使用稳定哈希，避免非 ASCII 字符被规范化后得到空编号。
 */
/**
 * 教程固定链接标识Deriver，属于教程与学习模块。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 */
public final class TutorialSlugDeriver {
    /**
     * 构造TutorialSlugDeriver，接收本类使用的依赖或初始化参数。
     */
    private TutorialSlugDeriver() {
    }

    /**
     * 从输入构造标题。
     * 关键处理步骤调用 <code>Normalizer.normalize</code>、<code>title.strip</code>、<code>normalized.isEmpty</code>、<code>normalized.length</code>。
     * 实现中的显式失败类型包括 <code>IllegalStateException</code>；具体触发条件见对应分支。
     * 边界判断包含 <code>(!normalized.isEmpty())</code>。
     *
     * @param title 标题；类型为 <code>String</code>
     * @param fallbackPrefix fallbackPrefix；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    public static String fromTitle(String title, String fallbackPrefix) {
        String normalized = Normalizer.normalize(title.strip(), Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (!normalized.isEmpty()) {
            return normalized.length() > 140 ? normalized.substring(0, 140).replaceAll("-+$", "") : normalized;
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(title.strip().getBytes(StandardCharsets.UTF_8));
            return fallbackPrefix + "-" + HexFormat.of().formatHex(digest, 0, 8);
        } catch (NoSuchAlgorithmException cause) {
            throw new IllegalStateException("JDK 缺少 SHA-256", cause);
        }
    }
}
