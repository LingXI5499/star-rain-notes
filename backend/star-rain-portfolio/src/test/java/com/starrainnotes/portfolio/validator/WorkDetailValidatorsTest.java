/*
 * WorkDetailValidatorsTest：作品模块的测试文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 * 主要协作类型：WorkInvalidException、MusicDetailValidator、OtherDetailValidator、SoftwareDetailValidator、VideoDetailValidator、WritingDetailValidator。
 * 阅读入口：eachTypeAcceptsItsOwnSchema（each类型AcceptsItsOwn数据库模式处理入口）；unknownFieldAndInvalidNumbersCannotEnterDetailJson（unknown字段及无效NumbersCannotEnter详情JSON处理入口）；node（node处理入口）。
 */

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

/**
 * 作品详情Validators测试，属于作品模块。
 * 以固定输入、模拟依赖或独立测试库验证生产代码，方法注释说明每个验证场景。
 */
class WorkDetailValidatorsTest {
    /**
     * JSON 读写或映射协作对象。
     * 协作类型：<code>ObjectMapper</code>。
     */
    private final ObjectMapper json = new ObjectMapper();

    /**
     * each类型AcceptsItsOwn数据库模式处理入口。
     * 关键处理步骤调用 <code>node</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
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

    /**
     * unknown字段及无效NumbersCannotEnter详情JSON处理入口。
     * 关键处理步骤调用 <code>node</code>。
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
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

    /**
     * node处理入口。
     * 主要协作调用：<code>json.readTree</code>。
     *
     * @param value 值；类型为 <code>String</code>
     * @return 类型为 <code>JsonNode</code> 的处理结果
     * @throws Exception 底层操作失败或输入不满足本方法要求时抛出
     */
    private JsonNode node(String value) throws Exception {
        return json.readTree(value);
    }
}
