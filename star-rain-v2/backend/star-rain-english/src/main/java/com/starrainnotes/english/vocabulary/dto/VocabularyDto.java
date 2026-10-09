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
public final class VocabularyDto {
    private VocabularyDto() { }

    @Data
    public static class Theme {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private String layer;
        private int layerOrder;
        private String name;
        private int sortOrder;
        private int wordCount;
    }

    @Data
    public static class Word {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long themeId;
        private String word;
        private String partOfSpeech;
        private String phoneticUs;
        private String phoneticUk;
        private String translation;
        /* 本主题用法：词卡「本主题用法」一行的来源 */
        private String sceneMeaning;
        /* 词形变化：词卡与学习页的「词形」一行 */
        private String inflections;
        private String examples;
        private int sortOrder;
        /*
         * 内容侧的记忆次数，只反映词条被记忆的累计统计，不是当前账户的个人进度。
         * 词卡上的「N 次记忆」取自家记忆体系（sr_english_vocabulary_memory）。
         */
        private int memoryCount;
        /* 已上传的授权发音，前端优先播放（无则回退后端代理与浏览器合成） */
        private List<VocabularyWordAudioVO> audios;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Page<T> {
        private List<T> items;
        private long total;
        private int page;
        private int size;
        /* 总页数，避免每个调用方各算一次；total=0 时为 0 */
        private int totalPages;
    }

    @Data
    public static class ThemeRequest {
        private String layer;
        private Integer layerOrder;
        private String name;
        private Integer sortOrder;
    }

    @Data
    public static class WordRequest {
        private String themeId;
        private String word;
        private String partOfSpeech;
        private String phoneticUs;
        private String phoneticUk;
        private String translation;
        /* 未提供时保留库中已有值，避免后台编辑把补充字段清空 */
        private String sceneMeaning;
        private String inflections;
        private String examples;
        private Integer sortOrder;
    }
}
