package com.starrainnotes.english.vocabulary.vo;

import com.starrainnotes.english.vocabulary.dto.VocabularyDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * 学习队列中的一个卡片：词条 + 个人记忆 + 本次复习方向 + 是否新词。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyStudyCardVO {

    private VocabularyDto.Word word;

    private VocabularyMemoryVO memory;

    /* EN_TO_ZH / ZH_TO_EN，MIXED 已在服务端解析成确定方向 */
    private String direction;

    private boolean newWord;
}
