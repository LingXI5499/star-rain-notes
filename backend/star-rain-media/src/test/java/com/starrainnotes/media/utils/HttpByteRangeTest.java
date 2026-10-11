/*
 * HttpByteRangeTest：媒体模块的测试文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：HttpByteRange、ApiException。
 * 阅读入口：fullContentWhenAbsent（\u6ca1\u6709 Range \u5934\u6216\u8bed\u6cd5\u65e0\u6cd5\u8bc6\u522b\u65f6\u6309\u5b8c\u6574\u5185\u5bb9\u54cd\u5e94）；explicitRange（bytes=start-end \u6b63\u5e38\u89e3\u6790）；openEndedRange（bytes=start- \u53d6\u5230\u672b\u5c3e）；suffixRange（bytes=-suffix \u53d6\u6700\u540e N \u5b57\u8282）。
 */

package com.starrainnotes.media.utils;

import com.starrainnotes.media.utils.HttpByteRange;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.starrainnotes.common.exception.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/*
 * HTTP Range 解析测试。
 * 音频/视频拖动进度依赖这段逻辑，算错区间会让播放器卡住或越界读文件。
 */
/**
 * HTTPByte范围测试，属于媒体模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class HttpByteRangeTest {

    /**
     * 总数。
     * 初始值为 <code>1000L</code>。
     */
    private static final long TOTAL = 1000L;

    /**
     * \u6ca1\u6709 Range \u5934\u6216\u8bed\u6cd5\u65e0\u6cd5\u8bc6\u522b\u65f6\u6309\u5b8c\u6574\u5185\u5bb9\u54cd\u5e94。
     * 关键处理步骤调用 <code>HttpByteRange.parse</code>。
     */
    @Test
    @DisplayName("没有 Range 头或语法无法识别时按完整内容响应")
    void fullContentWhenAbsent() {
        assertThat(HttpByteRange.parse(null, TOTAL)).isNull();
        assertThat(HttpByteRange.parse("", TOTAL)).isNull();
        assertThat(HttpByteRange.parse("items=0-10", TOTAL)).isNull();
        // 多区间不在支持范围，按完整内容响应而不是报错
        assertThat(HttpByteRange.parse("bytes=0-10,20-30", TOTAL)).isNull();
    }

    /**
     * bytes=start-end \u6b63\u5e38\u89e3\u6790。
     * 关键处理步骤调用 <code>HttpByteRange.parse</code>、<code>range.getStart</code>、<code>range.getEndInclusive</code>、<code>range.length</code>。
     */
    @Test
    @DisplayName("bytes=start-end 正常解析")
    void explicitRange() {
        HttpByteRange range = HttpByteRange.parse("bytes=100-199", TOTAL);

        assertThat(range.getStart()).isEqualTo(100);
        assertThat(range.getEndInclusive()).isEqualTo(199);
        assertThat(range.length()).isEqualTo(100);
        assertThat(range.contentRange(TOTAL)).isEqualTo("bytes 100-199/1000");
    }

    /**
     * bytes=start- \u53d6\u5230\u672b\u5c3e。
     * 关键处理步骤调用 <code>HttpByteRange.parse</code>、<code>range.getStart</code>、<code>range.getEndInclusive</code>、<code>range.length</code>。
     */
    @Test
    @DisplayName("bytes=start- 取到末尾")
    void openEndedRange() {
        HttpByteRange range = HttpByteRange.parse("bytes=900-", TOTAL);

        assertThat(range.getStart()).isEqualTo(900);
        assertThat(range.getEndInclusive()).isEqualTo(999);
        assertThat(range.length()).isEqualTo(100);
    }

    /**
     * bytes=-suffix \u53d6\u6700\u540e N \u5b57\u8282。
     * 关键处理步骤调用 <code>HttpByteRange.parse</code>、<code>range.getStart</code>、<code>range.getEndInclusive</code>、<code>range.length</code>。
     */
    @Test
    @DisplayName("bytes=-suffix 取最后 N 字节")
    void suffixRange() {
        HttpByteRange range = HttpByteRange.parse("bytes=-200", TOTAL);

        assertThat(range.getStart()).isEqualTo(800);
        assertThat(range.getEndInclusive()).isEqualTo(999);
        assertThat(range.length()).isEqualTo(200);
    }

    /**
     * suffix \u8d85\u8fc7\u8d44\u6e90\u957f\u5ea6\u65f6\u9000\u5316\u4e3a\u5b8c\u6574\u5185\u5bb9。
     * 关键处理步骤调用 <code>HttpByteRange.parse</code>、<code>range.getStart</code>、<code>range.getEndInclusive</code>、<code>range.length</code>。
     */
    @Test
    @DisplayName("suffix 超过资源长度时退化为完整内容")
    void suffixLargerThanResource() {
        HttpByteRange range = HttpByteRange.parse("bytes=-5000", TOTAL);

        assertThat(range.getStart()).isZero();
        assertThat(range.getEndInclusive()).isEqualTo(999);
        assertThat(range.length()).isEqualTo(TOTAL);
    }

    /**
     * \u53f3\u7aef\u8d85\u51fa\u8d44\u6e90\u957f\u5ea6\u65f6\u622a\u65ad\u5230\u672b\u5c3e\uff0c\u800c\u4e0d\u662f\u62a5\u9519。
     * 关键处理步骤调用 <code>HttpByteRange.parse</code>、<code>range.getStart</code>、<code>range.getEndInclusive</code>。
     */
    @Test
    @DisplayName("右端超出资源长度时截断到末尾，而不是报错")
    void endBeyondResourceIsClamped() {
        HttpByteRange range = HttpByteRange.parse("bytes=990-99999", TOTAL);

        assertThat(range.getStart()).isEqualTo(990);
        assertThat(range.getEndInclusive()).isEqualTo(999);
    }

    /**
     * \u8d77\u70b9\u8d8a\u754c\u3001\u533a\u95f4\u5012\u7f6e\u3001\u540e\u7f00\u4e3a 0\u3001\u975e\u6cd5\u6570\u5b57\u90fd\u8fd4\u56de 416。
     * 关键处理步骤调用 <code>HttpByteRange.parse</code>。
     */
    @Test
    @DisplayName("起点越界、区间倒置、后缀为 0、非法数字都返回 416")
    void unsatisfiableRanges() {
        for (String header : new String[]{"bytes=1000-", "bytes=1000-1200", "bytes=200-100",
                "bytes=-0", "bytes=abc-def", "bytes=10-abc"}) {
            assertThatThrownBy(() -> HttpByteRange.parse(header, TOTAL))
                    .as("header=%s", header)
                    .isInstanceOf(ApiException.class)
                    .extracting(ex -> ((ApiException) ex).getCode())
                    .isEqualTo("MEDIA_RANGE_INVALID");
        }
    }

    /**
     * \u5355\u5b57\u8282\u8d44\u6e90\u7684\u8fb9\u754c\u60c5\u51b5。
     * 关键处理步骤调用 <code>HttpByteRange.parse</code>、<code>range.length</code>。
     */
    @Test
    @DisplayName("单字节资源的边界情况")
    void singleByteResource() {
        HttpByteRange range = HttpByteRange.parse("bytes=0-0", 1L);

        assertThat(range.length()).isEqualTo(1);
        assertThatThrownBy(() -> HttpByteRange.parse("bytes=1-", 1L))
                .isInstanceOf(ApiException.class);
    }
}
