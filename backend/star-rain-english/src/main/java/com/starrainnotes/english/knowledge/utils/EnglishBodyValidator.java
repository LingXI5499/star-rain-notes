/*
 * EnglishBodyValidator：英语学习模块的工具文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 * 主要协作类型：ApiException。
 * 阅读入口：hasMeaningfulText（判断是否具备Meaningful文本）；requireBody（检查所需正文）；hash（摘要处理入口）；title（标题处理入口）。
 */

package com.starrainnotes.english.knowledge.utils;
import com.starrainnotes.common.exception.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;
/**
 * 英语正文校验器，属于英语学习模块。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 */
public final class EnglishBodyValidator {
    /**
     * 文本。
     */
    private static final Pattern TEXT=Pattern.compile("[\\p{L}\\p{N}]");
    /**
     * 构造EnglishBodyValidator，接收本类使用的依赖或初始化参数。
     */
    private EnglishBodyValidator() { }
    /**
     * 判断是否具备Meaningful文本。
     * 主要协作调用：<code>TEXT.matcher</code>。
     * 边界判断包含 <code>(markdown == null)</code>。
     *
     * @param markdown Markdown；类型为 <code>String</code>
     * @return 本方法条件是否成立的判断结果
     */
    public static boolean hasMeaningfulText(String markdown) {
        if(markdown==null) return false;
        String text=markdown.replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>","")
            .replaceAll("(?s)<[^>]*>","").replaceAll("!\\[[^\\]]*\\]\\([^)]*\\)","")
            .replaceAll("\\[([^\\]]*)\\]\\([^)]*\\)","$1")
            .replaceAll("(?m)^\\s*\\[[^]]+]:.*$","").replaceAll("&(?:nbsp|#160|#x[aA]0);", " ");
        return TEXT.matcher(text).find();
    }
    /**
     * 检查所需正文。
     * 关键处理步骤调用 <code>hasMeaningfulText</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     * 边界判断包含 <code>(!hasMeaningfulText(body))</code>。
     *
     * @param body 正文；类型为 <code>String</code>
     */
    public static void requireBody(String body) { if(!hasMeaningfulText(body)) throw new ApiException("ENGLISH_BODY_REQUIRED","完成或发布前请填写英文正文",400); }
    /**
     * 摘要处理入口。
     * 关键处理步骤调用 <code>HexFormat.of</code>、<code>MessageDigest.getInstance</code>。
     * 实现中的显式失败类型包括 <code>IllegalStateException</code>；具体触发条件见对应分支。
     *
     * @param text 文本；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    public static String hash(String text) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((text==null?"":text).getBytes(StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 unavailable",e); }
    }
    /**
     * 标题处理入口。
     * 关键处理步骤调用 <code>text.isBlank</code>、<code>text.trim</code>。
     *
     * @param text 文本；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    public static String title(String text) { return text==null || text.isBlank()?"Untitled Article":text.trim(); }
    /**
     * blank处理入口。
     *
     * @param text 文本；类型为 <code>String</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    public static String blank(String text) { return text==null?"":text; }
    /**
     * length处理入口。
     * 关键处理步骤调用 <code>value.length</code>。
     * 实现中的显式失败类型包括 <code>ApiException</code>；具体触发条件见对应分支。
     * 边界判断包含 <code>(value != null &amp;&amp; value.length() &gt; maximum)</code>。
     *
     * @param value 值；类型为 <code>String</code>
     * @param maximum maximum；类型为 <code>int</code>
     */
    public static void length(String value,int maximum) { if(value!=null && value.length()>maximum) throw new ApiException("ENGLISH_DOCUMENT_INVALID","内容超过允许长度",400); }
}
