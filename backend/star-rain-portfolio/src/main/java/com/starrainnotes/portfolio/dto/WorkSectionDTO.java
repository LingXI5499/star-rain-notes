/*
 * WorkSectionDTO：作品模块的请求与传输数据文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.portfolio.dto;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
/**
 * 作品区块传输对象，属于作品模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WorkSectionDTO {
    /**
     * 区块类型。
     */
    private String sectionType;
    /**
     * 标题。
     */
    private String title;
    /**
     * 内容。
     */
    private String content;
    /**
     * data。
     */
    private JsonNode data;
    /**
     * 区块版本。
     */
    private Integer blockVersion;
    /**
     * 可见。
     */
    private Boolean visible;
    /**
     * 媒体。
     */
    private List<SectionMediaDTO> media;
}
