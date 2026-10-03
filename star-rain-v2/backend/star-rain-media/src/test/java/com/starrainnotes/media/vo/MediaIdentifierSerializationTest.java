package com.starrainnotes.media.vo;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class MediaIdentifierSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

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
