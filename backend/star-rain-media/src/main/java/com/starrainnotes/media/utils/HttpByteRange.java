/*
 * HttpByteRange：媒体模块的工具文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 * 主要协作类型：MediaRangeInvalidException。
 * 阅读入口：length（length处理入口）；contentRange（内容范围处理入口）；parse（解析HTTPByte范围）。
 */

package com.starrainnotes.media.utils;

import com.starrainnotes.media.exception.MediaRangeInvalidException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 单区间 HTTP Range 请求的解析结果。
 * endInclusive 是闭区间右端，符合 RFC 9110 的 bytes=start-end 语义。
 */
/**
 * HTTPByte范围，属于媒体模块。
 * 封装本模块重复使用的转换、计算或校验步骤，具体边界条件由方法注释与实现保持一致。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HttpByteRange {

    /**
     * 开始。
     */
    private long start;
    /**
     * 结束Inclusive。
     */
    private long endInclusive;

    /**
     * length处理入口。
     * @return 类型为 <code>long</code> 的处理结果
     */
    public long length() {
        return endInclusive - start + 1;
    }

    /**
     * 内容范围处理入口。
     *
     * @param totalSize 总数大小；类型为 <code>long</code>
     * @return 类型为 <code>String</code> 的处理结果
     */
    public String contentRange(long totalSize) {
        return "bytes " + start + "-" + endInclusive + "/" + totalSize;
    }

    /*
     * 解析 Range 请求头。
     *
     * 返回 null 表示“按完整内容响应”（没有 Range 头，或语法无法识别时按规范降级为完整响应）。
     * 只有语法合法但区间超出资源长度时才抛 416。
     *
     * 支持三种形式：bytes=start-end、bytes=start-、bytes=-suffixLength。
     * 多区间请求（含逗号）不在支持范围，按完整内容响应而不是报错。
     */
    /**
     * 解析HTTPByte范围。
     * 关键处理步骤调用 <code>header.isBlank</code>、<code>header.trim</code>、<code>value.regionMatches</code>、<code>value.indexOf</code>。
     * 实现中的显式失败类型包括 <code>MediaRangeInvalidException</code>；具体触发条件见对应分支。
     * 边界判断包含 <code>(header == null || header.isBlank())</code>；<code>(!value.regionMatches(true, 0, "bytes=", 0, 6) || value.indexOf(',') &gt;= 0)</code>。
     *
     * @param header 响应头；类型为 <code>String</code>
     * @param totalSize 总数大小；类型为 <code>long</code>
     * @return 类型为 <code>HttpByteRange</code> 的处理结果
     */
    public static HttpByteRange parse(String header, long totalSize) {
        if (header == null || header.isBlank()) {
            return null;
        }
        String value = header.trim();
        if (!value.regionMatches(true, 0, "bytes=", 0, 6) || value.indexOf(',') >= 0) {
            return null;
        }
        String spec = value.substring(6).trim();
        int dash = spec.indexOf('-');
        if (dash < 0) {
            return null;
        }
        String startText = spec.substring(0, dash).trim();
        String endText = spec.substring(dash + 1).trim();

        if (startText.isEmpty()) {
            // bytes=-N：最后 N 字节
            long suffix = parseLong(endText);
            if (suffix <= 0) {
                throw new MediaRangeInvalidException();
            }
            long length = Math.min(suffix, totalSize);
            return new HttpByteRange(totalSize - length, totalSize - 1);
        }

        long start = parseLong(startText);
        if (start < 0 || start >= totalSize) {
            throw new MediaRangeInvalidException();
        }
        long end = endText.isEmpty() ? totalSize - 1 : parseLong(endText);
        if (end < start) {
            throw new MediaRangeInvalidException();
        }
        // 右端超出资源长度时截断到末尾，这是规范允许且推荐的处理
        return new HttpByteRange(start, Math.min(end, totalSize - 1));
    }

    /**
     * 解析Long。
     * 关键处理步骤调用 <code>Long.parseLong</code>。
     * 实现中的显式失败类型包括 <code>MediaRangeInvalidException</code>；具体触发条件见对应分支。
     *
     * @param text 文本；类型为 <code>String</code>
     * @return 类型为 <code>long</code> 的处理结果
     */
    private static long parseLong(String text) {
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ex) {
            throw new MediaRangeInvalidException();
        }
    }
}
