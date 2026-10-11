package com.starrainnotes.english.vocabulary.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 记忆集合中的一行：只带「是否已加入计划」所需的字段，避免为筛选拉取整份状态。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyMemoryEntryVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long wordId;

    private int memoryCount;

    private String lastMemoryAt;
}
