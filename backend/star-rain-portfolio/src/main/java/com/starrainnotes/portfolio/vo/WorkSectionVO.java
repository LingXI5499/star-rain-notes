/*
 * WorkSectionVO：作品模块的响应数据文件。
 * 业务范围：作品分类、展示区块、代码库链接及关联媒体。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */

package com.starrainnotes.portfolio.vo;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
/**
 * 作品区块响应对象，属于作品模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WorkSectionVO {
    /**
     * 当前业务对象的数据库标识。
     */
    private String id;
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
     * 展示排序值。
     */
    private Integer sortOrder;
    /**
     * 媒体。
     */
    private List<SectionMediaVO> media;
}
