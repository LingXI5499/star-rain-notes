package com.starrainnotes.portfolio.validator;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.portfolio.exception.WorkInvalidException;
import com.starrainnotes.portfolio.validator.impl.MusicDetailValidator;
import com.starrainnotes.portfolio.validator.impl.OtherDetailValidator;
import com.starrainnotes.portfolio.validator.impl.SoftwareDetailValidator;
import com.starrainnotes.portfolio.validator.impl.VideoDetailValidator;
import com.starrainnotes.portfolio.validator.impl.WritingDetailValidator;
import org.junit.jupiter.api.Test;

class WorkDetailValidatorsTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void eachTypeAcceptsItsOwnSchema() throws Exception {
        assertThatCode(() -> new SoftwareDetailValidator().validate(node("""
                {"techStack":["Java","Vue"],"role":"Full Stack","projectStage":"ONLINE"}
                """))).doesNotThrowAnyException();
        assertThatCode(() -> new VideoDetailValidator().validate(node("""
                {"platform":"Bilibili","durationSeconds":180}
                """))).doesNotThrowAnyException();
        assertThatCode(() -> new MusicDetailValidator().validate(node("""
                {"artistRole":"Composer","durationSeconds":240}
                """))).doesNotThrowAnyException();
        assertThatCode(() -> new WritingDetailValidator().validate(node("""
                {"publication":"Personal","wordCount":5000}
                """))).doesNotThrowAnyException();
        assertThatCode(() -> new OtherDetailValidator().validate(node("{}")))
                .doesNotThrowAnyException();
    }

    @Test
    void unknownFieldAndInvalidNumbersCannotEnterDetailJson() throws Exception {
        assertThatThrownBy(() -> new SoftwareDetailValidator().validate(node("""
                {"techStack":["Java"],"role":"Developer","projectStage":"ONLINE","admin":true}
                """))).isInstanceOf(WorkInvalidException.class);
        assertThatThrownBy(() -> new VideoDetailValidator().validate(node("""
                {"platform":"Bilibili","durationSeconds":-1}
                """))).isInstanceOf(WorkInvalidException.class);
        assertThatThrownBy(() -> new WritingDetailValidator().validate(node("""
                {"publication":"Personal","wordCount":"5000"}
                """))).isInstanceOf(WorkInvalidException.class);
    }

    private JsonNode node(String value) throws Exception {
        return json.readTree(value);
    }
}
