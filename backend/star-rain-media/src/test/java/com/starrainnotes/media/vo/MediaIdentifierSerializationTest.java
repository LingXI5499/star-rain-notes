/*
 * MediaIdentifierSerializationTest：媒体模块的测试文件。
 * 业务范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 阅读入口：serializesLargeIdentifiersAsExactStrings（serializesLargeIdentifiersAsExactStrings处理入口）。
 */

package com.starrainnotes.media.vo;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * 媒体IdentifierSerialization测试，属于媒体模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class MediaIdentifierSerializationTest {

    /**
     * 数据映射。
     * 协作类型：<code>ObjectMapper</code>。
     */
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * serializesLargeIdentifiersAsExactStrings处理入口。
     * 主要协作调用：<code>mapper.readTree</code>、<code>mapper.writeValueAsString</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    @Test
    void serializesLargeIdentifiersAsExactStrings() throws Exception {
        long id = 9007199254740993L;

        var summary = mapper.readTree(mapper.writeValueAsString(
                MediaAssetVO.builder().id(id).uploadedByAccountId(id).build()));
        assertThat(summary.get("id").asText()).isEqualTo("9007199254740993");
        assertThat(summary.get("id").isTextual()).isTrue();
        assertThat(summary.get("uploadedByAccountId").asText()).isEqualTo("9007199254740993");

        var detail = mapper.readTree(mapper.writeValueAsString(
                MediaAssetDetailVO.builder().id(id).uploadedByAccountId(id).build()));
        assertThat(detail.get("id").isTextual()).isTrue();
        assertThat(detail.get("uploadedByAccountId").isTextual()).isTrue();

        var reference = mapper.readTree(mapper.writeValueAsString(
                MediaReferenceVO.builder().id(id).mediaAssetId(id).sourceId(id).build()));
        assertThat(reference.get("id").asText()).isEqualTo("9007199254740993");
        assertThat(reference.get("mediaAssetId").isTextual()).isTrue();
        assertThat(reference.get("sourceId").isTextual()).isTrue();
    }
}
