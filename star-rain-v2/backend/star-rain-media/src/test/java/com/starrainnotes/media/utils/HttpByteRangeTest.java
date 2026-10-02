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
class HttpByteRangeTest {

    private static final long TOTAL = 1000L;

    @Test
    @DisplayName("没有 Range 头或语法无法识别时按完整内容响应")
    void fullContentWhenAbsent() {
        assertThat(HttpByteRange.parse(null, TOTAL)).isNull();
        assertThat(HttpByteRange.parse("", TOTAL)).isNull();
        assertThat(HttpByteRange.parse("items=0-10", TOTAL)).isNull();
        // 多区间不在支持范围，按完整内容响应而不是报错
        assertThat(HttpByteRange.parse("bytes=0-10,20-30", TOTAL)).isNull();
    }

    @Test
    @DisplayName("bytes=start-end 正常解析")
    void explicitRange() {
        HttpByteRange range = HttpByteRange.parse("bytes=100-199", TOTAL);

        assertThat(range.getStart()).isEqualTo(100);
        assertThat(range.getEndInclusive()).isEqualTo(199);
        assertThat(range.length()).isEqualTo(100);
        assertThat(range.contentRange(TOTAL)).isEqualTo("bytes 100-199/1000");
    }

    @Test
    @DisplayName("bytes=start- 取到末尾")
    void openEndedRange() {
        HttpByteRange range = HttpByteRange.parse("bytes=900-", TOTAL);

        assertThat(range.getStart()).isEqualTo(900);
        assertThat(range.getEndInclusive()).isEqualTo(999);
        assertThat(range.length()).isEqualTo(100);
    }

    @Test
    @DisplayName("bytes=-suffix 取最后 N 字节")
    void suffixRange() {
        HttpByteRange range = HttpByteRange.parse("bytes=-200", TOTAL);

        assertThat(range.getStart()).isEqualTo(800);
        assertThat(range.getEndInclusive()).isEqualTo(999);
        assertThat(range.length()).isEqualTo(200);
    }

    @Test
    @DisplayName("suffix 超过资源长度时退化为完整内容")
    void suffixLargerThanResource() {
        HttpByteRange range = HttpByteRange.parse("bytes=-5000", TOTAL);

        assertThat(range.getStart()).isZero();
        assertThat(range.getEndInclusive()).isEqualTo(999);
        assertThat(range.length()).isEqualTo(TOTAL);
    }

    @Test
    @DisplayName("右端超出资源长度时截断到末尾，而不是报错")
    void endBeyondResourceIsClamped() {
        HttpByteRange range = HttpByteRange.parse("bytes=990-99999", TOTAL);

        assertThat(range.getStart()).isEqualTo(990);
        assertThat(range.getEndInclusive()).isEqualTo(999);
    }

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

    @Test
    @DisplayName("单字节资源的边界情况")
    void singleByteResource() {
        HttpByteRange range = HttpByteRange.parse("bytes=0-0", 1L);

        assertThat(range.length()).isEqualTo(1);
        assertThatThrownBy(() -> HttpByteRange.parse("bytes=1-", 1L))
                .isInstanceOf(ApiException.class);
    }
}
