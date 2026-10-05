package com.starrainnotes.english.vocabulary.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 记忆集合行：只带「是否已加入计划」需要的字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyMemoryEntryRow {

    private Long wordId;

    private int memoryCount;

    private LocalDateTime lastMemoryAt;
}
