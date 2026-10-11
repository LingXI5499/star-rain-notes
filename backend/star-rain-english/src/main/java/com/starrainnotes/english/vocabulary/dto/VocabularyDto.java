/*
 * VocabularyDto：英语学习模块的请求与传输数据文件。
 * 业务范围：文档、词汇、语法、阅读听力及复习记录。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 * 主要协作类型：VocabularyWordAudioVO。
 */

package com.starrainnotes.english.vocabulary.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.starrainnotes.english.vocabulary.vo.VocabularyWordAudioVO;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 词汇内容侧传输对象：主题、单词、分页与后台写入请求。
 *
 * 全部使用 Lombok POJO（不用 record）：record 的访问器是 id() 而不是 getId()，
 * 与 BeanUtils、模板引擎等按 JavaBean 约定工作的库不兼容。
 */
/**
 * 词汇传输对象，属于英语学习模块。
 * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
 */
public final class VocabularyDto {
    /**
     * 构造VocabularyDto，接收本类使用的依赖或初始化参数。
     */
    private VocabularyDto() { }

    /**
     * 主题，属于英语学习模块。
     * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
     */
    @Data
    public static class Theme {
        /**
         * 当前业务对象的数据库标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        /**
         * layer。
         */
        private String layer;
        /**
         * layer顺序。
         */
        private int layerOrder;
        /**
         * 名称。
         */
        private String name;
        /**
         * 展示排序值。
         */
        private int sortOrder;
        /**
         * 单词数量。
         */
        private int wordCount;
    }

    /**
     * 单词，属于英语学习模块。
     * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
     */
    @Data
    public static class Word {
        /**
         * 当前业务对象的数据库标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        /**
         * 主题标识。
         * JSON 输出为字符串，避免 JavaScript 对大整数失去精度。
         */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long themeId;
        /**
         * 单词。
         */
        private String word;
        /**
         * part所属Speech。
         */
        private String partOfSpeech;
        /**
         * phoneticUs。
         */
        private String phoneticUs;
        /**
         * phoneticUk。
         */
        private String phoneticUk;
        /**
         * translation。
         */
        private String translation;
        /* 本主题用法：词卡「本主题用法」一行的来源 */
        /**
         * sceneMeaning。
         */
        private String sceneMeaning;
        /* 词形变化：词卡与学习页的「词形」一行 */
        /**
         * inflections。
         */
        private String inflections;
        /**
         * examples。
         */
        private String examples;
        /**
         * 展示排序值。
         */
        private int sortOrder;
        /*
         * 内容侧的记忆次数，只反映词条被记忆的累计统计，不是当前账户的个人进度。
         * 词卡上的「N 次记忆」取自家记忆体系（sr_english_vocabulary_memory）。
         */
        /**
         * 记忆数量。
         */
        private int memoryCount;
        /* 已上传的授权发音，前端优先播放（无则回退后端代理与浏览器合成） */
        /**
         * audios。
         */
        private List<VocabularyWordAudioVO> audios;
    }

    /**
     * 分页，属于英语学习模块。
     * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Page<T> {
        /**
         * 条目集合。
         */
        private List<T> items;
        /**
         * 符合查询条件的总记录数。
         */
        private long total;
        /**
         * 分页页码。
         */
        private int page;
        /**
         * 大小。
         */
        private int size;
        /* 总页数，避免每个调用方各算一次；total=0 时为 0 */
        /**
         * 总数Pages。
         */
        private int totalPages;
    }

    /**
     * 主题请求，属于英语学习模块。
     * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
     */
    @Data
    public static class ThemeRequest {
        /**
         * layer。
         */
        private String layer;
        /**
         * layer顺序。
         */
        private Integer layerOrder;
        /**
         * 名称。
         */
        private String name;
        /**
         * 展示排序值。
         */
        private Integer sortOrder;
    }

    /**
     * 单词请求，属于英语学习模块。
     * 描述请求、内部传输或计算结果的数据结构。字段约束使用本类的校验注解；没有注解的限制由使用该对象的服务处理。
     */
    @Data
    public static class WordRequest {
        /**
         * 主题标识。
         */
        private String themeId;
        /**
         * 单词。
         */
        private String word;
        /**
         * part所属Speech。
         */
        private String partOfSpeech;
        /**
         * phoneticUs。
         */
        private String phoneticUs;
        /**
         * phoneticUk。
         */
        private String phoneticUk;
        /**
         * translation。
         */
        private String translation;
        /* 未提供时保留库中已有值，避免后台编辑把补充字段清空 */
        /**
         * sceneMeaning。
         */
        private String sceneMeaning;
        /**
         * inflections。
         */
        private String inflections;
        /**
         * examples。
         */
        private String examples;
        /**
         * 展示排序值。
         */
        private Integer sortOrder;
    }
}
