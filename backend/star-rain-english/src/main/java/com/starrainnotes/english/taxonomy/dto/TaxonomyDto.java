/*
 * TaxonomyDto：英语学习模块的请求与传输数据文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */

package com.starrainnotes.english.taxonomy.dto;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
/**
 * Taxonomy传输对象，属于英语学习模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
public final class TaxonomyDto {
    /**
     * 构造TaxonomyDto，接收本类使用的依赖或初始化参数。
     */
    private TaxonomyDto() { }
    /**
     * Node，属于英语学习模块。
     * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
     */
    @Data public static class Node {
        /**
         * 当前业务对象的数据库标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using=ToStringSerializer.class) private Long id;
        /**
         * parent标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using=ToStringSerializer.class) private Long parentId;
        /**
         * dimension。
         */
        private String dimension;
        /**
         * 名称。
         */
        private String name;
        /**
         * 名称En。
         */
        private String nameEn;
        /**
         * 公开固定链接的路径标识。
         */
        private String slug;
        /**
         * 描述。
         */
        private String description;
        /**
         * 展示排序值。
         */
        private int sortOrder;
        /**
         * 已启用。
         */
        private boolean enabled;
        /**
         * worldBaseline。
         */
        private boolean worldBaseline;
        /**
         * children。
         */
        private List<Node> children = new ArrayList<>();
    }
}
