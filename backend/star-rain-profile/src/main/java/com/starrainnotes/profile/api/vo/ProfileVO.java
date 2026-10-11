/*
 * ProfileVO：作者资料模块的响应数据文件。
 * 业务范围：个人介绍、社交链接、经历技能和精选内容。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */

package com.starrainnotes.profile.api.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * 资料响应对象，属于作者资料模块。
 * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProfileVO {
    /**
     * 显示名称。
     */
    private String displayName;
    /**
     * headline。
     */
    private String headline;
    /**
     * bioMarkdown。
     */
    private String bioMarkdown;
    /**
     * location文本。
     */
    private String locationText;
    /**
     * avatar地址。
     */
    private String avatarUrl;
    /**
     * resume地址。
     */
    private String resumeUrl;
    /**
     * avatar媒体资产标识。
     */
    private String avatarMediaAssetId;
    /**
     * resume媒体资产标识。
     */
    private String resumeMediaAssetId;
    /**
     * experiences。
     */
    private List<Experience> experiences;
    /**
     * skills。
     */
    private List<Skill> skills;
    /**
     * socialLinks。
     */
    private List<SocialLink> socialLinks;
    /**
     * 精选Contents。
     */
    private List<Featured> featuredContents;

    /**
     * Experience，属于作者资料模块。
     * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
     */
    @Data @AllArgsConstructor
    public static class Experience {
        /**
         * 当前业务对象的数据库标识。
         */
        private String id;
        /**
         * experience类型。
         */
        private String experienceType;
        /**
         * 标题。
         */
        private String title;
        /**
         * organization。
         */
        private String organization;
        /**
         * 开始日期。
         */
        private LocalDate startDate;
        /**
         * 结束日期。
         */
        private LocalDate endDate;
        /**
         * 是否当前。
         */
        private Boolean isCurrent;
        /**
         * 描述Md。
         */
        private String descriptionMd;
        /**
         * 展示排序值。
         */
        private Integer sortOrder;
    }

    /**
     * Skill，属于作者资料模块。
     * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
     */
    @Data @AllArgsConstructor
    public static class Skill {
        /**
         * 当前业务对象的数据库标识。
         */
        private String id;
        /**
         * 分类。
         */
        private String category;
        /**
         * 名称。
         */
        private String name;
        /**
         * 描述。
         */
        private String description;
        /**
         * proficiency。
         */
        private String proficiency;
        /**
         * 展示排序值。
         */
        private Integer sortOrder;
    }

    /**
     * Social链接，属于作者资料模块。
     * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
     */
    @Data @AllArgsConstructor
    public static class SocialLink {
        /**
         * 当前业务对象的数据库标识。
         */
        private String id;
        /**
         * platform代码。
         */
        private String platformCode;
        /**
         * 标签。
         */
        private String label;
        /**
         * 地址。
         */
        private String url;
        /**
         * 展示排序值。
         */
        private Integer sortOrder;
    }

    /**
     * 精选，属于作者资料模块。
     * 描述返回给客户端的数据结构。转换方法负责把实体或内部结果映射到这些展示字段。
     */
    @Data @AllArgsConstructor
    public static class Featured {
        /**
         * 当前业务对象的数据库标识。
         */
        private String id;
        /**
         * 内容类型或请求声明的 Content-Type。
         */
        private String contentType;
        /**
         * 内容标识。
         */
        private String contentId;
        /**
         * 标题。
         */
        private String title;
        /**
         * 摘要。
         */
        private String summary;
        /**
         * 地址。
         */
        private String url;
        /**
         * 封面地址。
         */
        private String coverUrl;
        /**
         * available。
         */
        private Boolean available;
        /**
         * 展示排序值。
         */
        private Integer sortOrder;
    }
}
